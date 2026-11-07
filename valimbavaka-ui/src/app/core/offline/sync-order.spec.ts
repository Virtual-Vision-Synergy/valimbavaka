import { OutboxEntry } from './outbox-models';
import { SyncOrderRule, nextSendableEntry, syncLane } from './sync-order';

describe('syncLane', () => {
  it('should put journal, sales and invoices in the single operations group', () => {
    const lanes = ['/api/v1/journal', '/api/v1/sales/v-1', '/api/v1/invoices?print=true'].map(
      (url) => syncLane(url),
    );

    expect(new Set(lanes.map((lane) => lane.key))).toEqual(new Set(['group:operations']));
    expect(lanes.every((lane) => lane.kind === 'ORDERED')).toBe(true);
  });

  it('should give each record of a referential its own lane', () => {
    expect(syncLane('/api/v1/suppliers/s-1/status')).toEqual({
      key: 'record:/api/v1/suppliers/s-1',
      kind: 'INDEPENDENT',
    });
    expect(syncLane('/api/v1/suppliers/s-2').key).not.toBe(syncLane('/api/v1/suppliers/s-1').key);
  });

  it('should not confuse a prefix with a longer resource name', () => {
    expect(syncLane('/api/v1/stock-locations/l-1').kind).toBe('INDEPENDENT');
    expect(syncLane('/api/v1/stocks/movements').kind).toBe('ORDERED');
  });

  it('should leave an unknown address unclassified', () => {
    expect(syncLane('/api/v1/unknown').kind).toBe('UNCLASSIFIED');
  });

  it('should accept an added rule for a future write endpoint', () => {
    const rules: readonly SyncOrderRule[] = [{ prefix: '/api/v1/returns', kind: 'ORDERED' }];

    expect(syncLane('/api/v1/returns/r-1', rules).kind).toBe('ORDERED');
  });
});

describe('nextSendableEntry', () => {
  const entry = (id: string, url: string, extra: Partial<OutboxEntry> = {}): OutboxEntry => ({
    id,
    url,
    method: 'POST',
    body: {},
    createdAt: 1,
    attempts: 0,
    status: 'PENDING',
    ...extra,
  });
  const due = (blockedId: string) => (candidate: OutboxEntry) => candidate.id !== blockedId;

  it('should skip operations behind a waiting operation but not an independent record', () => {
    const entries = [
      entry('1', '/api/v1/sales'),
      entry('2', '/api/v1/invoices'),
      entry('3', '/api/v1/articles/a-1'),
    ];

    expect(nextSendableEntry(entries, due('1'))?.id).toBe('3');
  });

  it('should hold everything behind a waiting unclassified entry', () => {
    const entries = [entry('1', '/api/v1/items'), entry('2', '/api/v1/articles/a-1')];

    expect(nextSendableEntry(entries, due('1'))).toBeUndefined();
  });

  it('should hold operations behind a network failure but not behind a refusal', () => {
    const networkFailed = entry('1', '/api/v1/sales', { status: 'FAILED', networkFailure: true });
    const refused = { ...networkFailed, networkFailure: false };
    const next = entry('2', '/api/v1/invoices');

    expect(nextSendableEntry([networkFailed, next], () => true)).toBeUndefined();
    expect(nextSendableEntry([refused, next], () => true)?.id).toBe('2');
  });

  it('should return nothing for an empty queue', () => {
    expect(nextSendableEntry([], () => true)).toBeUndefined();
  });
});
