package com.spot.meet;

import android.content.Context;
import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

public class DatabaseHelper {

    private final FirebaseFirestore db;

    public DatabaseHelper(Context context) {
        db = FirebaseFirestore.getInstance();
    }

    public interface AvailabilityCallback {
        void onResult(boolean usernameTaken, boolean emailTaken);
    }

    public interface RegisterCallback {
        void onResult(boolean success);
    }

    public interface LoginCallback {
        void onSuccess(String email, String username, boolean isAdmin);
        void onAccountNotFound();
        void onWrongPassword();
        void onFailure();
    }

    public interface EmailLookupCallback {
        void onResult(String email);
    }

    public interface UpdateCallback {
        void onResult(boolean success);
    }

    public void checkAvailability(String username, String email, AvailabilityCallback callback) {
        String u = username.trim().toLowerCase();
        String e = email.trim().toLowerCase();

        Task<QuerySnapshot> usernameTask = db.collection("users").whereEqualTo("username", u).get();
        Task<QuerySnapshot> emailTask = db.collection("users").whereEqualTo("email", e).get();

        Tasks.whenAllComplete(usernameTask, emailTask).addOnCompleteListener(task -> {
            boolean uTaken = false;
            boolean eTaken = false;

            if (usernameTask.isSuccessful() && !usernameTask.getResult().isEmpty()) uTaken = true;
            if (emailTask.isSuccessful() && !emailTask.getResult().isEmpty()) eTaken = true;

            callback.onResult(uTaken, eTaken);
        });
    }

    public void registerUser(String username, String email, String password, RegisterCallback callback) {
        Map<String, Object> user = new HashMap<>();
        user.put("username", username.trim().toLowerCase());
        user.put("email", email.trim().toLowerCase());
        user.put("password", password);
        user.put("isAdmin", false);
        user.put("role", "user");

        db.collection("users").add(user)
                .addOnSuccessListener(doc -> callback.onResult(true))
                .addOnFailureListener(e -> callback.onResult(false));
    }


    public void loginUser(String identifier, String password, LoginCallback callback) {
        String id = identifier.trim().toLowerCase();

        
        String field = android.util.Patterns.EMAIL_ADDRESS.matcher(id).matches() ? "email" : "username";

        db.collection("users")
                .whereEqualTo(field, id)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (!task.getResult().isEmpty()) {
                            DocumentSnapshot doc = task.getResult().getDocuments().get(0);
                            String dbPassword = doc.getString("password");
                            
                            if (password.equals(dbPassword)) {
                                boolean isAdmin = false;
                                if (doc.contains("isAdmin")) {
                                    isAdmin = Boolean.TRUE.equals(doc.getBoolean("isAdmin"));
                                } else if ("admin".equals(doc.getString("username"))) {
                                    isAdmin = true;
                                }
                                callback.onSuccess(doc.getString("email"), doc.getString("username"), isAdmin);
                            } else {
                                callback.onWrongPassword();
                            }
                        } else {
                            callback.onAccountNotFound();
                        }
                    } else {
                        android.util.Log.e("FirestoreLogin", "Login Task Failed: ", task.getException());
                        callback.onFailure();
                    }
                });
    }

    public void getUserEmailByEmail(String email, EmailLookupCallback callback) {
        db.collection("users").whereEqualTo("email", email.trim().toLowerCase()).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        callback.onResult(task.getResult().getDocuments().get(0).getString("email"));
                    } else {
                        callback.onResult(null);
                    }
                });
    }

    public void getUserEmailByUsername(String username, EmailLookupCallback callback) {
        db.collection("users").whereEqualTo("username", username.trim().toLowerCase()).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        callback.onResult(task.getResult().getDocuments().get(0).getString("email"));
                    } else {
                        callback.onResult(null);
                    }
                });
    }


    public void updatePassword(String email, String newPassword, UpdateCallback callback) {
        db.collection("users").whereEqualTo("email", email.trim().toLowerCase()).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        DocumentReference docRef = task.getResult().getDocuments().get(0).getReference();
                        docRef.update("password", newPassword)
                                .addOnSuccessListener(aVoid -> callback.onResult(true))
                                .addOnFailureListener(e -> callback.onResult(false));
                    } else {
                        callback.onResult(false);
                    }
                });
    }
}
