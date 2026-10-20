export type JournalAction = 'CREATE' | 'UPDATE' | 'DELETE' | 'CANCEL' | 'REVERSE';

/** Ligne du journal append-only (JNL-01), miroir de `JournalEntryRDto`. */
export interface JournalEntryRDto {
  id: string;
  occurredAt: string;
  /** Absent pour une opération système. */
  actorId: string | null;
  actorName: string;
  action: JournalAction;
  resourceType: string;
  resourceId: string;
  description: string;
}
