# 项目 Cursor 规则集合

本目录包含了项目的开发规范和最佳实践，基于 [Awesome Cursor Rules](https://github.com/your-repo-url)。

## 规则目录结构

```
.cursorrules/
├── java/                      # Java 后端开发规则
│   ├── .cursorrules           # 主 Java 规则文件
│   ├── README.md              # Java 规则说明
│   └── springboot-jpa/     # Spring Boot JPA 开发规范
└── git/                      # Git 相关规则
    └── conventional-commits/  # 约定式提交规范
```

## 使用方法

### 在 Cursor 中使用规则

将 `.cursorrules` 文件可以直接放在项目根目录或相应子目录中，Cursor IDE 会自动加载并应用这些规则。

### 规则文件说明

#### Java 开发规则
- **`.cursorrules` - Java 后端开发主规则
- `springboot-jpa/` - Spring Boot + JPA 详细规范

#### Git 提交规范
- **`conventional-commits/` - 约定式提交规范

## 技术栈

- **Java**：17+
- **Spring Boot**：3.x
- **JPA/Hibernate**
- **Git**：约定式提交
