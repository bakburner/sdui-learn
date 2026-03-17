/**
 * AtomicElement — TypeScript types for the atomic rendering layer.
 * Mirrors the AtomicElement definition in sdui-schema.json.
 */

export interface AtomicPadding {
  top: number;
  end: number;
  bottom: number;
  start: number;
}

export type AtomicElementType =
  | 'Container'
  | 'Text'
  | 'Image'
  | 'Button'
  | 'Spacer'
  | 'Divider'
  | 'ScrollContainer'
  | 'Conditional'
  | 'DisplayGrid'
  | 'SectionSlot';

export interface AtomicElement {
  type: AtomicElementType;
  id?: string;

  // Container
  children?: AtomicElement[];
  direction?: 'row' | 'column';
  alignment?: 'start' | 'center' | 'end' | 'spaceBetween' | 'spaceAround' | 'spaceEvenly';
  crossAlignment?: 'start' | 'center' | 'end' | 'stretch';
  gap?: number;
  padding?: AtomicPadding;
  background?: any;

  // Text
  content?: string;
  variant?: string;
  weight?: string;
  color?: string;
  maxLines?: number;

  // Container layout
  flex?: number;
  breakpoint?: number;

  // Shared visual
  cornerRadius?: number;
  fillWidth?: boolean;

  // Image
  src?: string;
  aspectRatio?: number;
  fit?: 'cover' | 'contain' | 'fill' | 'fitWidth' | 'fitHeight' | 'none';
  placeholder?: string;
  width?: number;
  height?: number;

  // Button
  label?: string;
  buttonVariant?: 'filled' | 'outlined' | 'text';
  icon?: string;
  disabled?: boolean;

  // Spacer
  size?: number;

  // Divider
  orientation?: 'horizontal' | 'vertical';
  thickness?: number;

  // ScrollContainer
  paging?: boolean;
  snapAlignment?: 'start' | 'center' | 'end';

  // Conditional
  condition?: string;
  trueChild?: AtomicElement;
  falseChild?: AtomicElement;

  // DisplayGrid
  columns?: DisplayGridColumn[];
  rows?: Record<string, string>[];
  headerVariant?: string;
  cellVariant?: string;
  striped?: boolean;

  // SectionSlot — embedded section delegated back to SectionRouter
  section?: Record<string, unknown>;

  // Actions
  actions?: Record<string, unknown>[];
}

export interface DisplayGridColumn {
  key: string;
  label: string;
  align?: 'start' | 'center' | 'end';
  width?: number | 'flex';
}

export interface AtomicCompositeData {
  /** Rendering instructions — the atomic element tree */
  ui: AtomicElement;
  /** Optional domain data for future data-binding support */
  content?: Record<string, unknown>;
}
