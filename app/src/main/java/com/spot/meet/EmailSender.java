package com.spot.meet;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Random;


public class EmailSender {

    private static final String TAG = "EmailSender";
    private static final String ENDPOINT = "http://192.168.1.11:5001/send-email";

    public interface EmailCallback {
        void onSuccess(String code);
        void onFailure(String error);
    }

    
    public static String generateCode() {
        int half = new Random().nextInt(900) + 100; 
        return String.valueOf(half) + half;           
    }

    
    public static void sendResetCode(String toEmail, EmailCallback callback) {
        String code = generateCode();
        postEmail(
                toEmail,
                "SpotMeet – Your Password Reset Code",
                buildResetEmailBody(code, toEmail),
                code,
                callback
        );
    }


    public static void sendVerificationCode(String toEmail, String username, EmailCallback callback) {
        String code = generateCode();
        postEmail(
                toEmail,
                "SpotMeet – Verify Your Email Address",
                buildVerifyEmailBody(code, toEmail, username),
                code,
                callback
        );
    }


    private static String buildResetEmailBody(String code, String recipientEmail) {
        String half1 = code.substring(0, 3);
        String half2 = code.substring(3, 6);
        return "<!DOCTYPE html>" +
                "<html><head><meta charset='UTF-8'>" +
                "<style>" +
                "  body{margin:0;padding:0;background:#0D0D1A;font-family:'Segoe UI',Arial,sans-serif}" +
                "  .wrapper{max-width:560px;margin:40px auto;background:#1A1A2E;border-radius:20px;overflow:hidden;border:1px solid #3D3D6B}" +
                "  .header{background:linear-gradient(135deg,#E040FB,#7C4DFF);padding:36px 40px;text-align:center}" +
                "  .header h1{margin:0;color:#fff;font-size:28px;letter-spacing:2px;font-weight:800}" +
                "  .header p{margin:6px 0 0;color:rgba(255,255,255,.8);font-size:14px}" +
                "  .body{padding:40px;text-align:center}" +
                "  .greeting{color:#B0B0CC;font-size:15px;margin-bottom:24px;text-align:left}" +
                "  .code-label{color:#B388FF;font-size:12px;letter-spacing:3px;margin-bottom:14px}" +
                "  .code-box{display:inline-block;background:#16213E;border:2px solid #7C4DFF;border-radius:16px;padding:20px 40px}" +
                "  .code{font-size:48px;font-weight:900;letter-spacing:12px;color:#B388FF}" +
                "  .expire{color:#666699;font-size:13px;margin-top:20px}" +
                "  .warning{background:#1A0A2E;border-left:4px solid #E040FB;border-radius:8px;padding:14px 18px;text-align:left;margin-top:28px;color:#B0B0CC;font-size:13px}" +
                "  .footer{padding:24px 40px;border-top:1px solid #2A2A4A;text-align:center;color:#666699;font-size:12px}" +
                "</style></head><body>" +
                "<div class='wrapper'>" +
                "  <div class='header'><h1>⟨ SpotMeet ⟩</h1><p>Password Reset Request</p></div>" +
                "  <div class='body'>" +
                "    <p class='greeting'>Hello,<br><br>We received a request to reset the password for your SpotMeet account associated with <strong style='color:#B388FF'>" + recipientEmail + "</strong>.</p>" +
                "    <p class='code-label'>YOUR RESET CODE</p>" +
                "    <div class='code-box'><div class='code'>" + half1 + half2 + "</div></div>" +
                "    <p class='expire'>⏱ Expires in <strong style='color:#FFD740'>10 minutes</strong></p>" +
                "    <div class='warning'>🛡 <strong style='color:#E040FB'>Security Notice:</strong><br>If you did not request this, please ignore this email.</div>" +
                "  </div>" +
                "  <div class='footer'>© 2025 SpotMeet · Automated message — do not reply.</div>" +
                "</div></body></html>";
    }

    /** Green-themed account verification email body. */
    private static String buildVerifyEmailBody(String code, String recipientEmail, String username) {
        String half1 = code.substring(0, 3);
        String half2 = code.substring(3, 6);
        return "<!DOCTYPE html>" +
                "<html><head><meta charset='UTF-8'>" +
                "<style>" +
                "  body{margin:0;padding:0;background:#0D0D1A;font-family:'Segoe UI',Arial,sans-serif}" +
                "  .wrapper{max-width:560px;margin:40px auto;background:#1A1A2E;border-radius:20px;overflow:hidden;border:1px solid #1A3D2E}" +
                "  .header{background:linear-gradient(135deg,#00C853,#00BFA5);padding:36px 40px;text-align:center}" +
                "  .header h1{margin:0;color:#fff;font-size:28px;letter-spacing:2px;font-weight:800}" +
                "  .header p{margin:6px 0 0;color:rgba(255,255,255,.85);font-size:14px}" +
                "  .body{padding:40px;text-align:center}" +
                "  .greeting{color:#B0B0CC;font-size:15px;margin-bottom:24px;text-align:left}" +
                "  .username{color:#69FF47;font-weight:700}" +
                "  .code-label{color:#69FF47;font-size:12px;letter-spacing:3px;margin-bottom:14px}" +
                "  .code-box{display:inline-block;background:#0D1F15;border:2px solid #00C853;border-radius:16px;padding:20px 40px}" +
                "  .code{font-size:48px;font-weight:900;letter-spacing:12px;color:#69FF47}" +
                "  .expire{color:#666699;font-size:13px;margin-top:20px}" +
                "  .info{background:#0D1A10;border-left:4px solid #00C853;border-radius:8px;padding:14px 18px;text-align:left;margin-top:28px;color:#B0B0CC;font-size:13px}" +
                "  .steps{margin:20px 0;text-align:left;color:#B0B0CC;font-size:14px;line-height:1.8}" +
                "  .step-num{display:inline-block;width:24px;height:24px;border-radius:50%;background:#00C853;color:#000;font-weight:700;text-align:center;line-height:24px;margin-right:8px;font-size:13px}" +
                "  .footer{padding:24px 40px;border-top:1px solid #1A3D2E;text-align:center;color:#666699;font-size:12px}" +
                "</style></head><body>" +
                "<div class='wrapper'>" +
                "  <div class='header'><h1>✦ SpotMeet</h1><p>Activate Your Account</p></div>" +
                "  <div class='body'>" +
                "    <p class='greeting'>Welcome, <span class='username'>" + username + "</span>! 🎉<br><br>" +
                "    Thanks for joining SpotMeet. To activate your account at <strong style='color:#69FF47'>" + recipientEmail + "</strong>, enter the verification code below:</p>" +
                "    <p class='code-label'>YOUR VERIFICATION CODE</p>" +
                "    <div class='code-box'><div class='code'>" + half1 + half2 + "</div></div>" +
                "    <p class='expire'>⏱ Expires in <strong style='color:#FFD740'>10 minutes</strong></p>" +
                "    <div class='steps'>" +
                "      <div><span class='step-num'>1</span>Open SpotMeet</div>" +
                "      <div><span class='step-num'>2</span>Enter the 6-digit code above</div>" +
                "      <div><span class='step-num'>3</span>Your account will be activated!</div>" +
                "    </div>" +
                "    <div class='info'>📌 <strong style='color:#69FF47'>Did not sign up?</strong><br>You can safely ignore this email — no account will be created.</div>" +
                "  </div>" +
                "  <div class='footer'>© 2025 SpotMeet · Automated message — do not reply.</div>" +
                "</div></body></html>";
    }


    public static void sendEmail(String toEmail, String subject, String htmlBody, EmailCallback callback) {
        postEmail(toEmail, subject, htmlBody, "", callback);
    }

    private static void postEmail(String toEmail, String subject, String htmlBody,
                                  String code, EmailCallback callback) {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... voids) {
                HttpURLConnection conn = null;
                try {
                    URL url = new URL(ENDPOINT);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setRequestProperty("Accept", "application/json");
                    conn.setRequestProperty("ngrok-skip-browser-warning", "69420");
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0");
                    conn.setConnectTimeout(15000);
                    conn.setReadTimeout(15000);
                    conn.setDoOutput(true);

                    JSONObject json = new JSONObject();
                    json.put("to", toEmail);
                    json.put("subject", subject);
                    json.put("message", htmlBody);

                    byte[] body = json.toString().getBytes(StandardCharsets.UTF_8);
                    try (OutputStream os = conn.getOutputStream()) {
                        os.write(body);
                    }

                    int status = conn.getResponseCode();
                    Log.d(TAG, "Email send status: " + status);
                    return (status == 200 || status == 201) ? "OK" : "HTTP_ERROR:" + status;
                } catch (Exception e) {
                    Log.e(TAG, "Email send error", e);
                    return "EXCEPTION:" + e.getMessage();
                } finally {
                    if (conn != null) conn.disconnect();
                }
            }

            @Override
            protected void onPostExecute(String result) {
                if ("OK".equals(result)) callback.onSuccess(code);
                else callback.onFailure(result);
            }
        }.execute();
    }

    public static void sendBookingEmail(String toEmail, String username, String eventTitle, double lat, double lng, String reservationId, EmailCallback callback) {
        String googleMapLink = "https://www.google.com/maps/dir/?api=1&destination=" + lat + "," + lng;
        String qrCodeUrl = "https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=" + reservationId;
        
        String subject = "SpotMeet – Event Booking Confirmed: " + eventTitle;
        String htmlBody = "<!DOCTYPE html>" +
                "<html><head><meta charset='UTF-8'>" +
                "<style>" +
                "  body{margin:0;padding:0;background:#0D0D1A;font-family:'Segoe UI',Arial,sans-serif}" +
                "  .wrapper{max-width:560px;margin:40px auto;background:#1A1A2E;border-radius:20px;overflow:hidden;border:1px solid #3D3D6B}" +
                "  .header{background:linear-gradient(135deg,#2962FF,#00B0FF);padding:36px 40px;text-align:center}" +
                "  .header h1{margin:0;color:#fff;font-size:28px;letter-spacing:2px;font-weight:800}" +
                "  .header p{margin:6px 0 0;color:rgba(255,255,255,.8);font-size:14px}" +
                "  .body{padding:40px;text-align:center}" +
                "  .greeting{color:#B0B0CC;font-size:15px;margin-bottom:24px;text-align:left}" +
                "  .qr-box{margin:20px auto; padding:16px; background:#fff; display:inline-block; border-radius:12px;}" +
                "  .qr-box img{width:150px; height:150px; display:block;}" +
                "  .button{display:inline-block;margin-top:24px;padding:14px 28px;background:linear-gradient(135deg,#00C853,#00BFA5);color:#fff;text-decoration:none;border-radius:10px;font-weight:bold;letter-spacing:1px}" +
                "  .footer{padding:24px 40px;border-top:1px solid #2A2A4A;text-align:center;color:#666699;font-size:12px}" +
                "</style></head><body>" +
                "<div class='wrapper'>" +
                "  <div class='header'><h1>⟨ SpotMeet ⟩</h1><p>Booking Confirmed!</p></div>" +
                "  <div class='body'>" +
                "    <p class='greeting'>Hello " + username + ",<br><br>Your place for <strong>" + eventTitle + "</strong> is confirmed.</p>" +
                "    <div class='qr-box'><img src='" + qrCodeUrl + "' alt='QR Code' /></div>" +
                "    <p style='color:#B0B0CC;font-size:13px;'>Show this QR code at the entrance.<br>Reservation ID: " + reservationId + "</p>" +
                "    <a href='" + googleMapLink + "' class='button'>📍 OPEN IN GOOGLE MAPS</a>" +
                "  </div>" +
                "  <div class='footer'>© 2025 SpotMeet · Automated message — do not reply.</div>" +
                "</div></body></html>";
                
        postEmail(toEmail, subject, htmlBody, reservationId, callback);
    }

    /** Red-themed deletion notice for creators. */
    public static void sendDeletionNotice(String toEmail, String eventTitle, String reason, EmailCallback callback) {
        String subject = "SpotMeet – Event Removed: " + eventTitle;
        String htmlBody = "<!DOCTYPE html>" +
                "<html><head><meta charset='UTF-8'>" +
                "<style>" +
                "  body{margin:0;padding:0;background:#0D0D1A;font-family:'Segoe UI',Arial,sans-serif}" +
                "  .wrapper{max-width:560px;margin:40px auto;background:#1A1A2E;border-radius:20px;overflow:hidden;border:1px solid #4A1A1A}" +
                "  .header{background:linear-gradient(135deg,#FF5252,#FF1744);padding:36px 40px;text-align:center}" +
                "  .header h1{margin:0;color:#fff;font-size:28px;letter-spacing:2px;font-weight:800}" +
                "  .header p{margin:6px 0 0;color:rgba(255,255,255,.8);font-size:14px}" +
                "  .body{padding:40px;text-align:center}" +
                "  .greeting{color:#B0B0CC;font-size:15px;margin-bottom:24px;text-align:left}" +
                "  .reason-box{background:#2A1A1A;border-left:4px solid #FF5252;border-radius:8px;padding:20px;text-align:left;margin:24px 0;}" +
                "  .reason-label{color:#FF5252;font-size:12px;font-weight:bold;letter-spacing:1px;margin-bottom:8px;display:block;}" +
                "  .reason-text{color:#FFCDD2;font-size:15px;line-height:1.5;}" +
                "  .footer{padding:24px 40px;border-top:1px solid #2A2A4A;text-align:center;color:#666699;font-size:12px}" +
                "</style></head><body>" +
                "<div class='wrapper'>" +
                "  <div class='header'><h1>⚠️ SpotMeet</h1><p>Event Removal Notice</p></div>" +
                "  <div class='body'>" +
                "    <p class='greeting'>Hello,<br><br>We are writing to inform you that your event <strong>" + eventTitle + "</strong> has been removed from SpotMeet by our moderation team.</p>" +
                "    <div class='reason-box'>" +
                "      <span class='reason-label'>REASON FOR REMOVAL</span>" +
                "      <div class='reason-text'>" + reason + "</div>" +
                "    </div>" +
                "    <p style='color:#B0B0CC;font-size:14px;text-align:left;'>If you have questions regarding this action, please refer to our community guidelines or contact support.</p>" +
                "  </div>" +
                "  <div class='footer'>© 2025 SpotMeet · Automated message — do not reply.</div>" +
                "</div></body></html>";
        postEmail(toEmail, subject, htmlBody, "", callback);
    }

    /** Orange-themed cancellation notice for booked users. */
    public static void sendCancellationNotice(String toEmail, String eventTitle, EmailCallback callback) {
        String subject = "SpotMeet – Important: Event Cancelled: " + eventTitle;
        String htmlBody = "<!DOCTYPE html>" +
                "<html><head><meta charset='UTF-8'>" +
                "<style>" +
                "  body{margin:0;padding:0;background:#0D0D1A;font-family:'Segoe UI',Arial,sans-serif}" +
                "  .wrapper{max-width:560px;margin:40px auto;background:#1A1A2E;border-radius:20px;overflow:hidden;border:1px solid #4A3A1A}" +
                "  .header{background:linear-gradient(135deg,#FFAB40,#FF9100);padding:36px 40px;text-align:center}" +
                "  .header h1{margin:0;color:#fff;font-size:28px;letter-spacing:2px;font-weight:800}" +
                "  .header p{margin:6px 0 0;color:rgba(255,255,255,.8);font-size:14px}" +
                "  .body{padding:40px;text-align:center}" +
                "  .greeting{color:#B0B0CC;font-size:15px;margin-bottom:24px;text-align:left}" +
                "  .cancel-box{border:1px dashed #FFAB40;border-radius:12px;padding:24px;margin:24px 0;}" +
                "  .event-title{color:#FFAB40;font-size:20px;font-weight:bold;display:block;margin-bottom:8px;}" +
                "  .status{color:#FFD180;font-size:13px;letter-spacing:1px;font-weight:bold;}" +
                "  .footer{padding:24px 40px;border-top:1px solid #2A2A4A;text-align:center;color:#666699;font-size:12px}" +
                "</style></head><body>" +
                "<div class='wrapper'>" +
                "  <div class='header'><h1>🔔 SpotMeet</h1><p>Event Cancellation</p></div>" +
                "  <div class='body'>" +
                "    <p class='greeting'>Hello,<br><br>We are notifying you that an event you booked has been cancelled by the organizer or moderator.</p>" +
                "    <div class='cancel-box'>" +
                "      <span class='status'>STATUS: CANCELLED</span>" +
                "      <span class='event-title'>" + eventTitle + "</span>" +
                "    </div>" +
                "    <p style='color:#B0B0CC;font-size:14px;text-align:left;'>Any reservation you held for this event is now void. We apologize for any inconvenience caused.</p>" +
                "  </div>" +
                "  <div class='footer'>© 2025 SpotMeet · Automated message — do not reply.</div>" +
                "</div></body></html>";
        postEmail(toEmail, subject, htmlBody, "", callback);
    }
}
