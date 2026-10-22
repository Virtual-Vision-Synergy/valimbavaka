import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

const EAN13_LENGTH = 13;

/** Clé de contrôle EAN-13 : pondération 1/3 des 12 premiers chiffres. */
function checksumOk(code: string): boolean {
  const digits = [...code].map(Number);
  const sum = digits
    .slice(0, EAN13_LENGTH - 1)
    .reduce((total, digit, index) => total + digit * (index % 2 === 0 ? 1 : 3), 0);
  return (10 - (sum % 10)) % 10 === digits[EAN13_LENGTH - 1];
}

/**
 * Code-barres facultatif : vide accepté ; sinon 13 chiffres (`ean13Length` porte le nombre saisi)
 * et clé de contrôle valide (`ean13Checksum`).
 */
export const ean13Validator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const value = String(control.value ?? '').replace(/\s/g, '');
  if (!value) {
    return null;
  }
  if (!/^\d+$/.test(value) || value.length !== EAN13_LENGTH) {
    return { ean13Length: { actual: value.length } };
  }
  return checksumOk(value) ? null : { ean13Checksum: true };
};
