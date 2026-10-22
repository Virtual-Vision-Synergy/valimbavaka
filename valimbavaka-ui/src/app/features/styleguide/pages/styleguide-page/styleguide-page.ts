import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { Download, Plus, Trash2 } from 'lucide';
import { TranslatePipe } from '@ngx-translate/core';
import { UiPreferencesService } from '../../../../core/layout/ui-preferences-service';
import { AvatarInitials } from '../../../../shared/components/avatar-initials/avatar-initials';
import { BarChart, ChartBar, ChartTick } from '../../../../shared/components/bar-chart/bar-chart';
import { Icon } from '../../../../shared/components/icon/icon';
import { KpiCard } from '../../../../shared/components/kpi-card/kpi-card';
import { ListState } from '../../../../shared/components/list-state/list-state';
import { PageHeader } from '../../../../shared/components/page-header/page-header';
import { RangeTag } from '../../../../shared/components/range-tag/range-tag';
import {
  SegmentOption,
  SegmentedControl,
} from '../../../../shared/components/segmented-control/segmented-control';
import { StatusBadge } from '../../../../shared/components/status-badge/status-badge';
import { StepItem, Stepper } from '../../../../shared/components/stepper/stepper';
import { TabItem, Tabs } from '../../../../shared/components/tabs/tabs';
import { UnderConstruction } from '../../../../shared/components/under-construction/under-construction';

/** Jetons affichés en nuancier : nom de variable CSS et usage. */
const SWATCHES: readonly { token: string; usage: string }[] = [
  { token: '--primary', usage: 'Accent décoratif' },
  { token: '--primary-strong', usage: 'CTA, texte orange' },
  { token: '--secondary', usage: 'Barres, icônes' },
  { token: '--secondary-strong', usage: 'Texte teal, liens' },
  { token: '--secondary-deep', usage: 'Navigation' },
  { token: '--brun', usage: 'Canal téléphone' },
  { token: '--gris-anthracite', usage: 'Texte principal' },
  { token: '--gris-moyen', usage: 'Texte secondaire' },
  { token: '--fond', usage: 'Fond de page' },
  { token: '--succes-dark', usage: 'Succès' },
  { token: '--warning-dark', usage: 'Avertissement' },
  { token: '--danger-dark', usage: 'Erreur' },
];

/**
 * Guide de style vivant (développement uniquement) : jetons, élévations, verre et composants
 * partagés dans tous leurs états, en clair et en sombre. Sert de référence de validation visuelle.
 */
@Component({
  selector: 'vvs-styleguide-page',
  imports: [
    TranslatePipe,
    Icon,
    PageHeader,
    UnderConstruction,
    StatusBadge,
    RangeTag,
    KpiCard,
    Stepper,
    Tabs,
    SegmentedControl,
    BarChart,
    ListState,
    AvatarInitials,
  ],
  templateUrl: './styleguide-page.html',
  styleUrl: './styleguide-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StyleguidePage {
  private readonly preferences = inject(UiPreferencesService);

  protected readonly icons = { Download, Plus, Trash2 };
  protected readonly swatches = SWATCHES;
  protected readonly theme = this.preferences.theme;
  protected readonly tab = signal<'one' | 'two' | 'three'>('one');
  protected readonly tabs: readonly TabItem<'one' | 'two' | 'three'>[] = [
    { id: 'one', labelKey: 'dashboard.tab.overview' },
    { id: 'two', labelKey: 'dashboard.tab.analysis' },
    { id: 'three', labelKey: 'menu.item.reports' },
  ];
  protected readonly period = signal<'WEEK' | 'MONTH'>('WEEK');
  protected readonly periods: readonly SegmentOption<'WEEK' | 'MONTH'>[] = [
    { value: 'WEEK', labelKey: 'dashboard.chart.week' },
    { value: 'MONTH', labelKey: 'dashboard.chart.month' },
  ];
  protected readonly steps: readonly StepItem[] = [
    {
      labelKey: 'dashboard.step.OPENING.label',
      captionKey: 'dashboard.step.OPENING.caption',
      params: { time: '07:42' },
      state: 'done',
    },
    {
      labelKey: 'dashboard.step.SALES.label',
      captionKey: 'dashboard.step.SALES.caption',
      params: { tickets: 47 },
      state: 'current',
    },
    {
      labelKey: 'dashboard.step.CLOSING.label',
      captionKey: 'dashboard.step.CLOSING.caption',
      params: { time: '19:00' },
      state: 'upcoming',
    },
  ];
  protected readonly bars: readonly ChartBar[] = [
    { label: 'Lun', value: 980_000, display: '0,98 M' },
    { label: 'Mar', value: 1_460_000, display: '1,46 M' },
    { label: 'Mer', value: 640_000, display: '0,64 M' },
    { label: 'Jeu', value: 1_284_000, display: '1,28 M', highlight: true },
  ];
  protected readonly ticks: readonly ChartTick[] = [
    { value: 1_500_000, label: '1,5 M' },
    { value: 1_000_000, label: '1 M' },
    { value: 500_000, label: '0,5 M' },
    { value: 0, label: '0' },
  ];

  protected toggleTheme(): void {
    this.preferences.toggleTheme();
  }
}
