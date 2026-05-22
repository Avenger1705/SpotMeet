import os
import uuid
import smtplib
from email.mime.text import MIMEText
from email.mime.multipart import MIMEMultipart
from flask import Flask, request, jsonify, send_from_directory
from dotenv import load_dotenv

# Load environment variables from .env file
load_dotenv()

app = Flask(__name__)

# --- CONFIGURATION ---
SENDER_EMAIL = os.getenv("SENDER_EMAIL", "")
APP_PASSWORD = os.getenv("APP_PASSWORD", "")

# Setup the 'files' directory inside the same folder as this script
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
FILES_DIR = os.path.join(BASE_DIR, "files")
os.makedirs(FILES_DIR, exist_ok=True)

ALLOWED_EXTENSIONS = {"jpg", "jpeg", "png", "webp", "gif"}

# --- HELPERS ---
def allowed_file(filename):
    return "." in filename and filename.rsplit(".", 1)[1].lower() in ALLOWED_EXTENSIONS

# --- ROUTES ---
@app.route("/")
def home():
    return "SpotMeet Server is running"

# 1. Email Sending functionality
@app.route("/send-email", methods=["POST"])
def send_email():
    try:
        data = request.get_json(silent=True)

        if not data:
            return jsonify({
                "success": False,
                "message": "No JSON received"
            }), 400

        receiver_email = data.get("to", "").strip()
        subject = data.get("subject", "").strip()
        message = data.get("message", "").strip()

        if not receiver_email or not subject or not message:
            return jsonify({
                "success": False,
                "message": "to, subject, and message are required"
            }), 400

        msg = MIMEMultipart("alternative")
        msg["Subject"] = subject
        msg["From"] = f"SpotMeet <{SENDER_EMAIL}>"
        msg["To"] = receiver_email
        msg.attach(MIMEText(message, "html", "utf-8"))

        with smtplib.SMTP("smtp.gmail.com", 587) as server:
            server.ehlo()
            server.starttls()
            server.login(SENDER_EMAIL, APP_PASSWORD)
            server.sendmail(SENDER_EMAIL, receiver_email, msg.as_string())

        return jsonify({
            "success": True,
            "message": f"Email sent to {receiver_email}"
        })

    except Exception as e:
        print(f"[EMAIL ERROR]: {e}")
        return jsonify({
            "success": False,
            "message": str(e)
        }), 500


# 2. Image Uploading functionality
# 2. Image Uploading functionality
@app.route("/upload-image", methods=["POST"])
def upload_image():
    try:
        if "image" not in request.files:
            return jsonify({"success": False, "message": "No image field in request"}), 400

        file = request.files["image"]
        if file.filename == "":
            return jsonify({"success": False, "message": "Empty filename"}), 400

        if file and allowed_file(file.filename):
            ext = file.filename.rsplit(".", 1)[1].lower()
            filename = f"{uuid.uuid4().hex}.{ext}"
            save_path = os.path.join(FILES_DIR, filename)

            file.save(save_path)

            # --- GENERATE THUMBNAIL ---
            thumb_filename = f"thumb_{filename}"
            thumb_path = os.path.join(FILES_DIR, thumb_filename)
            try:
                from PIL import Image
                with Image.open(save_path) as img:
                    # Maintain aspect ratio, max 400px
                    img.thumbnail((400, 400))
                    # Handle transparency if saving as JPEG
                    if img.mode in ("RGBA", "P"):
                        img = img.convert("RGB")
                    img.save(thumb_path, "JPEG", quality=85)
                has_thumb = True
            except Exception as e:
                print(f"[THUMB ERROR]: {e}. Make sure 'pip install Pillow' is run.")
                has_thumb = False

            print(f"[UPLOAD SUCCESS]: {filename}")

            base_url = f"http://{request.host}"
            full_url = f"{base_url}/files/{filename}"
            thumb_url = f"{base_url}/files/{thumb_filename}" if has_thumb else full_url

            return jsonify({
                "success": True,
                "url": full_url,
                "thumbnail_url": thumb_url
            }), 200
        else:
            return jsonify({"success": False, "message": "File type not allowed"}), 400

    except Exception as e:
        print(f"[UPLOAD ERROR]: {e}")
        return jsonify({"success": False, "message": str(e)}), 500


# 3. Viewing/Downloading Images
@app.route("/files/<filename>")
def serve_file(filename):
    return send_from_directory(FILES_DIR, filename)


if __name__ == "__main__":
    print("=" * 60)
    print("  SpotMeet API Server")
    print("  Port: 5001")
    print(f"  Storage: {FILES_DIR}")
    print("=" * 60)
    app.run(host="0.0.0.0", port=5001, debug=False, use_reloader=False, threaded=True)