import { Directive, ElementRef, forwardRef, HostListener, inject } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

@Directive({
  selector: 'input[appDigitsOnly]',
  standalone: true,
  host: { inputmode: 'numeric', pattern: '[0-9]*' },
  providers: [{ provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => DigitsOnlyDirective), multi: true }]
})
export class DigitsOnlyDirective implements ControlValueAccessor {
  private input = inject(ElementRef<HTMLInputElement>).nativeElement;
  private change: (value: string) => void = () => {};
  private touched: () => void = () => {};
  writeValue(value: string | null): void { this.input.value = value ?? ''; }
  registerOnChange(fn: (value: string) => void): void { this.change = fn; }
  registerOnTouched(fn: () => void): void { this.touched = fn; }
  setDisabledState(disabled: boolean): void { this.input.disabled = disabled; }
  @HostListener('input') onInput(): void {
    const position = this.input.selectionStart ?? this.input.value.length;
    const before = this.input.value.slice(0, position).replace(/\D/g, '');
    this.input.value = this.input.value.replace(/\D/g, '').slice(0, 15);
    this.input.setSelectionRange(before.length, before.length);
    this.change(this.input.value);
  }
  @HostListener('blur') onBlur(): void { this.touched(); }
}
