package com.example.qrorderkitchen.data.repository

import com.example.qrorderkitchen.data.models.StaffModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

sealed class AuthResult {
    data class Success(val staff: StaffModel) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

class AuthRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    suspend fun authenticateStaff(usernameInput: String, pinInput: String): AuthResult {
        val username = usernameInput.trim().lowercase()
        val pin = pinInput.trim()

        if (username.isBlank() || pin.isBlank()) {
            return AuthResult.Error("Vui lòng nhập đầy đủ Tên đăng nhập và Mã PIN!")
        }

        return try {
            // 1. Đồng bộ đăng nhập Firebase Authentication trước để thỏa mãn Firestore Rules isAuthenticated()
            val effectiveEmail = if (username.contains("@")) username else "${username.lowercase()}@aurora.internal"
            val effectivePass = if (pin.length >= 6) pin else "123456"

            try {
                auth.signInWithEmailAndPassword(effectiveEmail, effectivePass).await()
            } catch (authErr: Exception) {
                android.util.Log.w("AuthRepo", "FirebaseAuth note: ${authErr.message}")
            }

            // 2. Tra cứu thông tin hồ sơ nhân viên trong collection staff theo username
            val snapshot = db.collection("staff")
                .whereEqualTo("username", username)
                .get()
                .await()

            if (snapshot.isEmpty) {
                AuthResult.Error("Tên đăng nhập hoặc mã PIN không chính xác!")
            } else {
                val doc = snapshot.documents.first()
                val staff = doc.toObject(StaffModel::class.java)?.apply { id = doc.id }

                val pinSalt = "AURORA_PIN_SALT_2026_"
                val computedHash = java.security.MessageDigest.getInstance("SHA-256")
                    .digest((pinSalt + pin).toByteArray(Charsets.UTF_8))
                    .joinToString("") { "%02x".format(it) }

                val dbHash = doc.getString("pin_hash")
                val dbPin = doc.getString("pin")
                val isMatched = (dbHash != null && dbHash.equals(computedHash, ignoreCase = true)) || (dbPin != null && dbPin == pin)

                if (!isMatched) {
                    AuthResult.Error("Tên đăng nhập hoặc mã PIN không chính xác!")
                } else if (staff == null) {
                    AuthResult.Error("Không thể đọc thông tin tài khoản nhân viên!")
                } else if (staff.status.equals("locked", ignoreCase = true) || staff.status.equals("inactive", ignoreCase = true)) {
                    AuthResult.Error("Tài khoản nhân viên này đang bị tạm khóa!")
                } else if (staff.platform.equals("web", ignoreCase = true)) {
                    AuthResult.Error("Tài khoản này chỉ được cấp phép trên Web Quản trị!")
                } else {
                    // Tự động nâng cấp mã băm SHA-256 nếu tài khoản cũ chưa có pin_hash
                    if (dbHash.isNullOrBlank()) {
                        try {
                            doc.reference.update("pin_hash", computedHash)
                        } catch (_: Exception) {}
                    }
                    AuthResult.Success(staff)
                }
            }
        } catch (e: Exception) {
            AuthResult.Error("Lỗi kết nối máy chủ: ${e.localizedMessage}")
        }
    }
}
