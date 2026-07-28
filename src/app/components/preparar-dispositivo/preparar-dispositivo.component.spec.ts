import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { IonicModule } from '@ionic/angular';

import { PrepararDispositivoComponent } from './preparar-dispositivo.component';

describe('PrepararDispositivoComponent', () => {
  let component: PrepararDispositivoComponent;
  let fixture: ComponentFixture<PrepararDispositivoComponent>;
  let router: Router;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      imports: [RouterTestingModule, IonicModule.forRoot(), PrepararDispositivoComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(PrepararDispositivoComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    fixture.detectChanges();
  }));

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should navigate to the Wi-Fi setup flow', () => {
    const navigateSpy = spyOn(router, 'navigate');

    component.next();

    expect(navigateSpy).toHaveBeenCalledWith(['/wifi']);
  });
});
