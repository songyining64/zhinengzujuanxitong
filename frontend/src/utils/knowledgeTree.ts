import type { KnowledgePoint } from '@/types/models';

export interface TreeNode {
  id: number;
  label: string;
  /** 与知识点对应的说明文字 */
  content?: string;
  children?: TreeNode[];
}

export function buildKnowledgeTree(points: KnowledgePoint[]): TreeNode[] {
  const byParent = new Map<number | null, KnowledgePoint[]>();
  for (const p of points) {
    const k = p.parentId ?? null;
    if (!byParent.has(k)) byParent.set(k, []);
    byParent.get(k)!.push(p);
  }
  for (const arr of byParent.values()) {
    arr.sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0));
  }
  function walk(parent: number | null): TreeNode[] {
    return (byParent.get(parent) ?? []).map((c) => ({
      id: c.id,
      label: c.name,
      content: c.content,
      children: walk(c.id).length ? walk(c.id) : undefined
    }));
  }
  return walk(null);
}
