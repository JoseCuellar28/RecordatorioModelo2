package com.example.recordatoriomodelo2.services

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage
import android.util.Log

object EmailService {
    
    // Configuración del servidor SMTP (Gmail)
    private const val SMTP_HOST = "smtp.gmail.com"
    private const val SMTP_PORT = "587"
    private const val EMAIL_FROM = "elemate0916@gmail.com"
    private const val EMAIL_PASSWORD = "mhjrnwdcuqlaurwq"
    
    suspend fun sendTemporaryPasswordEmail(
        toEmail: String,
        fullName: String,
        temporaryPassword: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        Log.d("EmailService", "=== INICIO sendTemporaryPasswordEmail ===")
        Log.d("EmailService", "Parámetros: toEmail=$toEmail, fullName=$fullName, temporaryPassword=$temporaryPassword")
        
        try {
            // Configuración real del SMTP para envío de correos
            Log.d("EmailService", "Configurando SMTP para envío real con Gmail...")
            
            val props = Properties().apply {
                put("mail.smtp.host", SMTP_HOST)
                put("mail.smtp.port", SMTP_PORT)
                put("mail.smtp.auth", "true")
                put("mail.smtp.starttls.enable", "true")
                put("mail.smtp.ssl.trust", SMTP_HOST)
            }
            
            val session = Session.getInstance(props, object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(EMAIL_FROM, EMAIL_PASSWORD)
                }
            })
            
            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(EMAIL_FROM))
                setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail))
                subject = "Contraseña Temporal - Sistema de Recordatorios"
                
                val htmlContent = """
                    <html>
                    <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                        <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                            <h2 style="color: #2196F3; text-align: center;">¡Bienvenido al Sistema de Recordatorios!</h2>
                            
                            <p>Hola <strong>$fullName</strong>,</p>
                            
                            <p>Tu cuenta ha sido creada exitosamente. Para acceder al sistema, utiliza la siguiente contraseña temporal:</p>
                            
                            <div style="background-color: #f5f5f5; padding: 15px; border-radius: 5px; text-align: center; margin: 20px 0;">
                                <h3 style="color: #2196F3; margin: 0; font-size: 24px; letter-spacing: 2px;">$temporaryPassword</h3>
                            </div>
                            
                            <div style="background-color: #fff3cd; border: 1px solid #ffeaa7; padding: 15px; border-radius: 5px; margin: 20px 0;">
                                <p style="margin: 0; color: #856404;"><strong>⚠️ Importante:</strong></p>
                                <ul style="margin: 10px 0; color: #856404;">
                                    <li>Esta contraseña es temporal y debe ser cambiada en tu primer inicio de sesión</li>
                                    <li>Por seguridad, no compartas esta contraseña con nadie</li>
                                    <li>Si no fuiste tú quien solicitó esta cuenta, ignora este correo</li>
                                </ul>
                            </div>
                            
                            <p>Para iniciar sesión:</p>
                            <ol>
                                <li>Abre la aplicación</li>
                                <li>Ingresa tu email: <strong>$toEmail</strong></li>
                                <li>Ingresa la contraseña temporal mostrada arriba</li>
                                <li>Sigue las instrucciones para crear tu contraseña permanente</li>
                            </ol>
                            
                            <hr style="border: none; border-top: 1px solid #eee; margin: 30px 0;">
                            
                            <p style="text-align: center; color: #666; font-size: 12px;">
                                Este es un correo automático, por favor no responder.<br>
                                Sistema de Recordatorios © 2024
                            </p>
                        </div>
                    </body>
                    </html>
                """.trimIndent()
                
                setContent(htmlContent, "text/html; charset=utf-8")
            }
            
            Log.d("EmailService", "Enviando correo con contraseña temporal...")
            Transport.send(message)
            Log.d("EmailService", "=== CORREO ENVIADO EXITOSAMENTE ===")
            
            // Log para desarrollo (mantener para debugging)
            Log.d("EmailService", "=== DETALLES DEL CORREO ===")
            Log.d("EmailService", "Para: $toEmail")
            Log.d("EmailService", "Nombre: $fullName")
            Log.d("EmailService", "Contraseña temporal: $temporaryPassword")
            Log.d("EmailService", "========================")
            
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e("EmailService", "=== ERROR EN sendTemporaryPasswordEmail ===")
            Log.e("EmailService", "Excepción: ${e.message}")
            Log.e("EmailService", "Tipo de excepción: ${e.javaClass.simpleName}")
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    suspend fun sendPasswordChangeNotification(
        toEmail: String,
        fullName: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val props = Properties().apply {
                put("mail.smtp.host", SMTP_HOST)
                put("mail.smtp.port", SMTP_PORT)
                put("mail.smtp.auth", "true")
                put("mail.smtp.starttls.enable", "true")
                put("mail.smtp.ssl.trust", SMTP_HOST)
            }
            
            val session = Session.getInstance(props, object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(EMAIL_FROM, EMAIL_PASSWORD)
                }
            })
            
            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(EMAIL_FROM))
                setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail))
                subject = "Contraseña Actualizada - Sistema de Recordatorios"
                
                val htmlContent = """
                    <html>
                    <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                        <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                            <h2 style="color: #4CAF50; text-align: center;">Contraseña Actualizada</h2>
                            
                            <p>Hola <strong>$fullName</strong>,</p>
                            
                            <p>Tu contraseña ha sido actualizada exitosamente en el Sistema de Recordatorios.</p>
                            
                            <div style="background-color: #d4edda; border: 1px solid #c3e6cb; padding: 15px; border-radius: 5px; margin: 20px 0;">
                                <p style="margin: 0; color: #155724;"><strong>✅ Confirmación:</strong></p>
                                <p style="margin: 10px 0; color: #155724;">
                                    Tu cuenta ahora está completamente configurada y puedes usar tu nueva contraseña para futuros inicios de sesión.
                                </p>
                            </div>
                            
                            <p>Si no realizaste este cambio, contacta inmediatamente al administrador del sistema.</p>
                            
                            <hr style="border: none; border-top: 1px solid #eee; margin: 30px 0;">
                            
                            <p style="text-align: center; color: #666; font-size: 12px;">
                                Este es un correo automático, por favor no responder.<br>
                                Sistema de Recordatorios © 2024
                            </p>
                        </div>
                    </body>
                    </html>
                """.trimIndent()
                
                setContent(htmlContent, "text/html; charset=utf-8")
            }
            
            Transport.send(message)
            Result.success(Unit)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}