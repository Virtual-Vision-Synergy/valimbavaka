import { Injectable } from '@angular/core';

/**
 * Remise d'une facture au client depuis le navigateur (FAC-03), sans serveur de messagerie : enregistrement du PDF,
 * partage du fichier par le menu du système (WhatsApp, Messenger, courriel… sur téléphone) ou, à défaut, message
 * `mailto:` à compléter par la vendeuse. Isolé dans un service pour que les tests n'ouvrent ni fenêtre ni menu.
 */
@Injectable({ providedIn: 'root' })
export class InvoiceDelivery {
  /** Enregistre le fichier sous le nom donné (téléchargement du navigateur). */
  save(file: Blob, filename: string): void {
    const url = URL.createObjectURL(file);
    const link = document.createElement('a');
    link.href = url;
    link.download = filename;
    link.click();
    URL.revokeObjectURL(url);
  }

  /** Vrai si le navigateur sait partager ce fichier (surtout les navigateurs de téléphone). */
  canShare(file: File): boolean {
    return typeof navigator.canShare === 'function' && navigator.canShare({ files: [file] });
  }

  /** Ouvre le menu de partage du système ; la promesse est rejetée si la vendeuse l'abandonne. */
  share(file: File, title: string, text: string): Promise<void> {
    return navigator.share({ files: [file], title, text });
  }

  /** Ouvre la messagerie avec un message prérempli, sans destinataire (l'adresse du client est inconnue). */
  openMail(subject: string, body: string): void {
    window.location.href = `mailto:?subject=${encodeURIComponent(subject)}&body=${encodeURIComponent(body)}`;
  }
}
