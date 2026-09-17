> 原文：[每个开发人员都应该知道的 20 个 TypeScript 技巧](https://juejin.cn/post/7429384221670735881) · 作者：龙丽坤

# 20 个必知的 TypeScript 技巧

## 1. NonNullable（排除 null / undefined）

`NonNullable<T>` 排除 null 和 undefined，避免意外空值。

```ts
type User = { name: string; age?: number | null };
const user: NonNullable<User["age"]> = 30; // ✅ 不允许 null / undefined
```

## 2. Partial（属性全可选）

更新对象子集时很有用。

```ts
interface User { name: string; age: number; email: string; }
const updateUser = (user: Partial<User>) => ({ ...user, updatedAt: new Date() });
updateUser({ name: 'John' }); // 不需要全部字段
```

## 3. Readonly（不可变）

所有属性只读，防止重新赋值。

```ts
const config: Readonly<{ apiUrl: string; retries: number }> = { apiUrl: 'https://api.example.com', retries: 5 };
config.apiUrl = 'https://newapi.com'; // ❌ 报错
```

## 4. 映射类型（Mapped Types）

基于现有类型生成新类型。

```ts
type Status = 'loading' | 'success' | 'error';
type ApiResponse<T> = { [K in Status]: T };
```

## 5. 元组可选元素

元组支持可选元素，适合可变参数。

```ts
type UserTuple = [string, number?, boolean?];
const u1: UserTuple = ['Alice'];
const u2: UserTuple = ['Bob', 30];
```

## 6. 联合类型的穷尽检查

switch 里用 `never` 做穷尽性校验，新增类型漏处理会编译报错。

```ts
function handleStatus(status: 'open' | 'closed' | 'pending') {
  switch (status) {
    case 'open': return 'Opened';
    case 'closed': return 'Closed';
    case 'pending': return 'Pending';
    default:
      const _exhaustive: never = status; // 新增状态未处理 → 报错
      return _exhaustive;
  }
}
```

## 7. Omit（排除属性）

从类型中移除指定键。

```ts
interface Todo { title: string; description: string; completed: boolean; }
type TodoPreview = Omit<Todo, 'description'>;
```

## 8. in / instanceof 类型收窄

按运行时条件收窄类型。

```ts
function processInput(input: string | number | { title: string }) {
  if (typeof input === 'string') return input.toUpperCase();
  else if (typeof input === 'number') return input * 2;
  else if ('title' in input) return input.title;
}
```

## 9. 条件类型

根据条件转换类型。

```ts
type IsString<T> = T extends string ? true : false;
type Check = IsString<'Hello'>; // true
```

## 10. as const（不可变字面量）

冻结值并当作字面量类型而非可变值。

```ts
const COLORS = ['red', 'green', 'blue'] as const;
type Color = typeof COLORS[number]; // 'red' | 'green' | 'blue'
```

## 11. Extract / Exclude（提取 / 排除联合成员）

```ts
type T = 'a' | 'b' | 'c';
type OnlyAOrB = Extract<T, 'a' | 'b'>; // 'a' | 'b'
type ExcludeC = Exclude<T, 'c'>;       // 'a' | 'b'
```

## 12. 自定义类型保护（Type Guard）

运行时动态收窄类型。

```ts
function isString(input: any): input is string {
  return typeof input === 'string';
}
if (isString(value)) console.log(value.toUpperCase()); // 安全
```

## 13. Record（动态键对象）

```ts
type Role = 'admin' | 'user' | 'guest';
const permissions: Record<Role, string[]> = {
  admin: ['read', 'write', 'delete'],
  user: ['read', 'write'],
  guest: ['read'],
};
```

## 14. 索引签名（动态类属性）

```ts
class DynamicObject { [key: string]: any; }
const obj = new DynamicObject();
obj.name = 'Alice'; obj.age = 30;
```

## 15. never（不可能出现的状态）

常用于穷尽检查。

```ts
function assertNever(value: never): never {
  throw new Error(`Unexpected value: ${value}`);
}
```

## 16. 可选链（?.）

安全访问深层属性，避免 undefined 报错。

```ts
const userName = user?.profile?.name;
const age = user?.profile?.age ?? 'Not provided'; // 兜底
```

## 17. 空值合并（??）

左侧为 null / undefined 时才返回右侧。

```ts
const input: string | null = null;
const v = input ?? 'Default'; // 'Default'
```

## 18. ReturnType（推断返回类型）

```ts
function getUser() { return { name: 'John', age: 30 }; }
type UserReturn = ReturnType<typeof getUser>; // { name: string; age: number; }
```

## 19. 函数泛型参数

让函数在不同类型间复用。

```ts
function identity<T>(value: T): T { return value; }
identity<string>('Hello');
identity<number>(42);
```

## 20. 交叉类型（&）

把多个类型合并成一个。

```ts
type Admin = { privileges: string[] };
type User = { name: string };
type AdminUser = Admin & User;
```

## 总结

灵活运用这些技巧，能写出更安全、更健壮、更易维护的 TypeScript 代码，更好地发挥类型系统的威力。
