import { TestBed } from '@angular/core/testing';

import { Wifi } from './wifi';

describe('Wifi', () => {
  let service: Wifi;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(Wifi);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
