import { TestBed } from '@angular/core/testing';
import { ProductRange } from '../../models/tone';
import { RangeTag } from './range-tag';

async function render(range: ProductRange): Promise<HTMLElement> {
  await TestBed.configureTestingModule({ imports: [RangeTag] }).compileComponents();
  const fixture = TestBed.createComponent(RangeTag);
  fixture.componentRef.setInput('range', range);
  await fixture.whenStable();
  return (fixture.nativeElement as HTMLElement).querySelector('span') as HTMLElement;
}

describe('RangeTag', () => {
  it.each<ProductRange>(['BOTANIKA', 'ZEZIKA', 'SOLIKA'])(
    'should expose data-range and the label text for %s',
    async (range) => {
      const tag = await render(range);

      expect(tag.getAttribute('data-range')).toBe(range);
      expect(tag.textContent?.trim()).toBe(range);
    },
  );
});
