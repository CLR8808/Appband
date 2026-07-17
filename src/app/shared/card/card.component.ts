import { Component, Input, Output, EventEmitter } from '@angular/core';
import { IonicModule } from '@ionic/angular';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-card',
  templateUrl: './card.component.html',
  styleUrls: ['./card.component.scss'],
  standalone: true,
  imports: [IonicModule, CommonModule]
})
export class CardComponent {
  @Input() icon: string = '';
  @Input() title: string = '';
  @Input() danger: boolean = false;
  @Input() showChevron: boolean = true;
  @Input() showSwitch: boolean = false;
  @Input() switchValue: boolean = false;
  
  @Output() switchChange = new EventEmitter<boolean>();
  @Output() onClick = new EventEmitter<void>();

  handleClick() {
    if (!this.showSwitch) {
      this.onClick.emit();
    }
  }

  onToggle(event: any) {
    this.switchValue = event.detail.checked;
    this.switchChange.emit(this.switchValue);
  }
}
