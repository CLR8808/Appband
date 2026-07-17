import { TestBed } from '@angular/core/testing';

import { Maps } from './maps';

describe('Maps', () => {
  let service: Maps;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(Maps);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
