import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { PageResponse } from '../../../shared/models/page-response';
import { RoleService } from './role-service';
import { RoleRDto, UserRDto } from './user-models';
import { UserService } from './user-service';

describe('UserService', () => {
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should request a page with the VVS pagination parameters and skip an empty search', () => {
    let result: PageResponse<UserRDto> | undefined;
    TestBed.inject(UserService)
      .list({ page: 2, size: 10, search: '' })
      .subscribe((page) => (result = page));

    const request = httpMock.expectOne((req) => req.url === '/api/v1/users');
    expect(request.request.params.get('page')).toBe('2');
    expect(request.request.params.get('size')).toBe('10');
    expect(request.request.params.has('search')).toBe(false);
    const page: PageResponse<UserRDto> = {
      items: [],
      page: 2,
      pageSize: 10,
      totalCount: 0,
      totalPages: 0,
    };
    request.flush(page);

    expect(result).toEqual(page);
  });
});

describe('RoleService', () => {
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should return the business roles from the first page', () => {
    const roles: RoleRDto[] = [
      { id: '1', name: 'ADMINISTRATEUR', description: 'Administrateur' },
      { id: '2', name: 'LIVREUR', description: 'Livreur' },
      { id: '3', name: 'VENDEUSE', description: 'Vendeuse' },
    ];
    let result: RoleRDto[] | undefined;
    TestBed.inject(RoleService)
      .listAll()
      .subscribe((items) => (result = items));

    httpMock
      .expectOne((req) => req.url === '/api/v1/roles')
      .flush({ items: roles, page: 1, pageSize: 100, totalCount: 3, totalPages: 1 });

    expect(result?.map((role) => role.name)).toEqual(['ADMINISTRATEUR', 'LIVREUR', 'VENDEUSE']);
  });
});
