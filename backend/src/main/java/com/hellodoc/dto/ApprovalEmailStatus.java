package com.hellodoc.dto;

import com.hellodoc.entity.DoctorApplication;
import java.time.Instant;

public record ApprovalEmailStatus(String status,String message,Instant attemptedAt,Instant sentAt) {
    public static ApprovalEmailStatus from(DoctorApplication application) {
        String status=application.getApprovalEmailStatus();
        String message=switch(status) {
            case "SENT" -> "Le courriel d’approbation a été transmis au serveur de messagerie.";
            case "QUEUED" -> "L’envoi du courriel d’approbation est en cours.";
            case "DISABLED" -> "L’envoi des courriels est désactivé dans la configuration du backend.";
            case "FAILED" -> switch(application.getApprovalEmailError()==null?"":application.getApprovalEmailError()) {
                case "AUTHENTICATION" -> "Le serveur SMTP refuse l’authentification. Vérifiez l’adresse et le mot de passe d’application Google dans IntelliJ, puis redémarrez le backend.";
                case "CONNECTION" -> "Le serveur de messagerie est injoignable ou ne répond pas. Vérifiez le réseau, l’hôte et le port SMTP.";
                case "CONFIGURATION" -> "La configuration SMTP est incomplète ou invalide. Vérifiez les variables du backend puis redémarrez-le.";
                default -> "Le courriel n’a pas pu être envoyé. Vérifiez la configuration SMTP et réessayez.";
            };
            default -> "Aucun envoi du courriel d’approbation n’est enregistré pour ce compte.";
        };
        return new ApprovalEmailStatus(status,message,application.getApprovalEmailAttemptedAt(),application.getApprovalEmailSentAt());
    }
}
