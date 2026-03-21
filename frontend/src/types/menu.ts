export interface MenuTreeVO {
  id: number;
  parentId?: number | null;
  name: string;
  path?: string | null;
  icon?: string | null;
  sortOrder?: number | null;
  perms?: string | null;
  children: MenuTreeVO[];
}
