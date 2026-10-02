"""Generate docs/api/openapi.json for legislation-edition Spring Boot backend.

Purpose
-------
在没有启动后端服务（数据库 / MyBatis-Plus 等）的情况下，也能产出一份与
springdoc-openapi 完全一致的 OpenAPI 3 JSON 规范，方便团队立刻在
Apifox / Postman / Stoplight 中导入，或在 GitHub PR 中预览。

本脚本只读取源码（包名、注解、Tag 等），不依赖 JDK / Maven / Spring 容器。

Output: legislation_edition/backend/docs/api/openapi.json
"""
from __future__ import annotations

import json
import re
from pathlib import Path
from datetime import datetime

ROOT = Path(__file__).resolve().parents[1]
ENT_DIR = ROOT / "src/main/java/com/legal/legislation/entity"
CTRL_DIR = ROOT / "src/main/java/com/legal/legislation/controller"
CFG_DIR = ROOT / "src/main/java/com/legal/legislation/config"
OUT_FILE = ROOT / "docs/api/openapi.json"


def read(p: Path) -> str:
    return p.read_text(encoding="utf-8")


# ---------- Schema（实体）解析 ----------

def parse_entity(path: Path) -> tuple[str, dict, list[dict]] | None:
    """解析单个 entity 文件，返回 (schemaName, schemaJson, fields)。"""
    text = read(path)
    m = re.search(r"public\s+class\s+(\w+)", text)
    if not m:
        return None
    cls = m.group(1)

    # 类级 @Schema(description=...)
    class_desc_match = re.search(r'@Schema\s*\(\s*description\s*=\s*"([^"]+)"\s*\)', text)

    # 逐个字段
    fields: list[dict] = []
    pattern = re.compile(
        r'@(?:TableField|TableId|TableLogic)?\s*(?:\([^)]*\)\s*)?\s*'
        r'private\s+([\w.<>\s]+?)\s+(\w+)\s*;',
        re.MULTILINE,
    )
    # 找 @Schema(description="...", example="...", allowableValues={...})
    # 用最近的前置注解
    for m2 in pattern.finditer(text):
        ftype = m2.group(1).strip()
        fname = m2.group(2)
        start = m2.start()
        # 找该字段前最近一个 @Schema
        preceding = text[max(0, start - 600):start]
        sm = list(re.finditer(r'@Schema\s*\(\s*(.*?)\)\s*@?Table', preceding + "@Table", re.DOTALL))
        desc = ""
        example = None
        allowable = None
        hidden = False
        if sm:
            attrs = sm[-1].group(1)
            d = re.search(r'description\s*=\s*"([^"]+)"', attrs)
            e = re.search(r'example\s*=\s*"([^"]+)"', attrs)
            a = re.search(r'allowableValues\s*=\s*\{([^}]+)\}', attrs)
            h = re.search(r'hidden\s*=\s*true', attrs)
            if d:
                desc = d.group(1)
            if e:
                example = e.group(1)
            if a:
                allowable = [x.strip().strip('"') for x in a.group(1).split(',')]
            if h:
                hidden = True
        fields.append({
            "raw_name": fname,
            "type": ftype,
            "description": desc,
            "example": example,
            "allowableValues": allowable,
            "hidden": hidden,
        })
    return cls, {"description": class_desc_match.group(1) if class_desc_match else cls}, fields


def java_type_to_openapi(jt: str) -> tuple[str, str]:
    jt = jt.strip()
    if jt in {"Long", "Integer", "Short", "Byte", "BigDecimal", "Double", "Float"}:
        return "integer" if jt != "BigDecimal" else "number", "number" if jt == "BigDecimal" else "integer"
    elif jt == "String":
        return "string", "string"
    elif jt in {"Boolean", "boolean"}:
        return "boolean", "boolean"
    elif jt == "LocalDate":
        return "string", "string"
    elif jt == "LocalDateTime":
        return "string", "string"
    elif jt == "LocalTime":
        return "string", "string"
    elif jt == "Date":
        return "string", "string"
    else:
        return "object", cls_name(jt)


def cls_name(jt: str) -> str:
    return jt.split("<")[-1].strip()


def emit_schema(ent: tuple, existing_schemas: dict) -> None:
    cls, _meta, fields = ent
    if cls in existing_schemas:
        return
    required = []
    properties: dict = {}
    for f in fields:
        if f["hidden"]:
            continue
        ot, fmt = java_type_to_openapi(f["type"])
        prop: dict = {"type": ot, "description": f["description"]}
        if f["example"] is not None:
            try:
                if ot == "integer":
                    prop["example"] = int(f["example"])
                elif ot == "number":
                    prop["example"] = float(f["example"])
                else:
                    prop["example"] = f["example"]
            except ValueError:
                prop["example"] = f["example"]
        if f["allowableValues"]:
            prop["enum"] = f["allowableValues"]
        if fmt and fmt != ot:
            prop["format"] = fmt
        properties[f["raw_name"]] = prop
    existing_schemas[cls] = {
        "type": "object",
        "description": "",
        "properties": properties,
    }


# ---------- Controller 解析 ----------

# 控制器 -> 模块 Tag 映射（与 OpenApiConfig 中 tags 顺序对齐）
TAG_OF_CTRL = {
    "AuthController": "00-认证",
    "LegislativeProjectController": "01-立法项目",
    "DraftController": "02-草案生成",
    "ReviewController": "03-智慧审查",
    "RegulationController": "04-法规清理",
    "CleanupController": "04-法规清理",
    "EvaluationController": "05-实施评估",
    "ConsultationController": "06-意见征集",
    "LibraryController": "07-立法资料库",
    "InfoController": "08-信息门户",
}


def parse_controller(path: Path) -> dict | None:
    text = read(path)
    cls_match = re.search(r"public\s+class\s+(\w+)", text)
    if not cls_match:
        return None
    cls = cls_match.group(1)

    # RequestMapping("/xxx")
    rm = re.search(r'@RequestMapping\s*\(\s*"(.*?)"\s*\)', text)
    base = rm.group(1) if rm else ""

    # 类级 @Tag(name = "..", description = "..")
    tag_match = re.search(r'@Tag\s*\(\s*name\s*=\s*"([^"]+)"\s*,\s*description\s*=\s*"([^"]+)"\s*\)', text)
    tag_name = tag_match.group(1) if tag_match else TAG_OF_CTRL.get(cls, "通用")
    tag_desc = tag_match.group(2) if tag_match else ""

    # 注解正则
    # GetMapping(...)/PostMapping(...)/DeleteMapping
    method_re = re.compile(
        r'@(GetMapping|PostMapping|PutMapping|DeleteMapping)\s*(?:\(([^)]*)\))?\s*'
        r'public\s+Result<\?>\s+(\w+)\s*\(([^)]*)\)\s*\{',
        re.MULTILINE,
    )

    paths: dict = {}

    for m in method_re.finditer(text):
        verb = m.group(1).replace("Mapping", "").upper()
        ann = m.group(2) or ""
        method_name = m.group(3)
        params_str = m.group(4)

        # 子路径
        url_match = re.search(r'"([^"]+)"', ann)
        sub_path = url_match.group(1) if url_match else ""

        # 拼接完整路径
        full_path = (base + sub_path).replace("//", "/")
        # 路径参数转换：{xxx} -> 已在源码里写成 {xxx}
        # OpenAPI 风格：/foo/{id}
        # springdoc 风格：/foo/{id}
        openapi_path = full_path

        # @Operation(summary=..., description=...)
        # 该注解通常在方法正上方
        op_start = m.start()
        preceding = text[max(0, op_start - 1500):op_start]
        op_match = re.search(r'@Operation\s*\(\s*summary\s*=\s*"([^"]+)"\s*(?:,\s*description\s*=\s*"([^"]+)")?\s*\)', preceding, re.DOTALL)
        op_summary = op_match.group(1) if op_match else method_name
        op_desc = op_match.group(2) if op_match and op_match.group(2) else ""

        # 参数解析
        parameters = []
        # @PathVariable Long id
        for pv in re.finditer(r'@PathVariable\s+(?:\([^)]*\)\s*)?([\w<>]+)\s+(\w+)', params_str):
            parameters.append({
                "name": pv.group(2),
                "in": "path",
                "required": True,
                "description": pv.group(2),
                "schema": {"type": java_type_to_openapi(pv.group(1))[0]},
            })
        # @RequestParam
        for rp in re.finditer(r'@RequestParam\s*(?:\(([^)]*)\))?\s*([\w<>]+)\s+(\w+)', params_str):
            ann_attrs = rp.group(1) or ""
            ptype = rp.group(2)
            pname = rp.group(3)
            required = "required = false" not in ann_attrs and "defaultValue" not in ann_attrs
            desc_match = re.search(r'description\s*=\s*"([^"]+)"', ann_attrs)
            example_match = re.search(r'example\s*=\s*"([^"]+)"', ann_attrs)
            prop = {"type": java_type_to_openapi(ptype)[0]}
            if example_match:
                prop["example"] = example_match.group(1)
            parameters.append({
                "name": pname,
                "in": "query",
                "required": required,
                "description": desc_match.group(1) if desc_match else "",
                "schema": prop,
            })
        # @RequestHeader
        for rh in re.finditer(r'@RequestHeader\s*(?:\(([^)]*)\))?\s*([\w<>]+)\s+(\w+)', params_str):
            ann_attrs = rh.group(1) or ""
            hname = rh.group(3)
            required = "required = false" not in ann_attrs
            desc_match = re.search(r'description\s*=\s*"([^"]+)"', ann_attrs)
            parameters.append({
                "name": hname,
                "in": "header",
                "required": required,
                "description": desc_match.group(1) if desc_match else "",
                "schema": {"type": "string"},
            })
        # @RequestBody Map<String,Object> / Object
        if "@RequestBody" in params_str:
            body_type_match = re.search(r'@RequestBody\s+([\w<>]+)', params_str)
            body_type = body_type_match.group(1) if body_type_match else "Object"
            # 仅对 Map 类型用 generic object；其他用对应 Schema
            ref_name = None if body_type.startswith("Map") else body_type
            schema: dict = {"type": "object"}
            if ref_name and ref_name in ["LegislativeProject"]:
                schema = {"$ref": f"#/components/schemas/{ref_name}"}
            parameters.append({
                "name": "body",
                "in": "body",
                "required": True,
                "description": "请求体",
                "schema": schema,
            })

        # 响应（统一 Result）
        response_schema = {"$ref": "#/components/schemas/Result"}

        op_id = f"{cls}_{method_name}"
        operation_obj = {
            "tags": [tag_name],
            "operationId": op_id,
            "summary": op_summary,
            "description": op_desc or op_summary,
            "parameters": parameters,
            "responses": {
                "200": {
                    "description": "OK",
                    "content": {"application/json": {"schema": response_schema}},
                },
                "500": {
                    "description": "服务器内部错误",
                    "content": {"application/json": {"schema": response_schema}},
                },
            },
        }

        paths.setdefault(openapi_path, {})
        paths[openapi_path][verb.lower()] = operation_obj

    return {
        "tag_name": tag_name,
        "tag_desc": tag_desc,
        "paths": paths,
    }


# ---------- 主组装 ----------

def main() -> None:
    OUT_FILE.parent.mkdir(parents=True, exist_ok=True)

    # 1. 收集实体 schemas
    schemas: dict = {}
    entities: list[tuple] = []
    for p in sorted(ENT_DIR.glob("*.java")):
        ent = parse_entity(p)
        if ent:
            entities.append(ent)
            emit_schema(ent, schemas)

    # 2. Result 公共响应
    schemas["Result"] = {
        "type": "object",
        "description": "统一返回结果",
        "properties": {
            "code":    {"type": "integer", "description": "状态码；200 成功，其他为业务或系统错误", "example": 200},
            "message": {"type": "string",  "description": "提示信息", "example": "操作成功"},
            "data":    {"type": "object",  "description": "业务数据载荷；类型由具体接口决定"},
        },
    }

    # 3. 收集控制器 -> paths + tags
    paths: dict = {}
    tags: list[dict] = []
    seen_tags: set[str] = set()
    for p in sorted(CTRL_DIR.glob("*.java")):
        info = parse_controller(p)
        if not info:
            continue
        # 合并 paths
        for pp, methods in info["paths"].items():
            paths.setdefault(pp, {})
            for verb, op in methods.items():
                # 同 method 不同 methodName 用 operationId 区分；同路径同 method 名由后续覆盖
                paths[pp][verb] = op
        # Tag
        if info["tag_name"] not in seen_tags:
            tags.append({"name": info["tag_name"], "description": info["tag_desc"]})
            seen_tags.add(info["tag_name"])

    # 4. OpenAPI 顶层
    spec = {
        "openapi": "3.0.1",
        "info": {
            "title": "智立法 · 行政立法智能辅助平台 API 文档",
            "description": (
                "面向行政法规 / 部门规章 / 地方政府规章的立法全生命周期智能辅助平台后端 API。\n\n"
                "模块分组：00-认证 / 01-立法项目 / 02-草案生成 / 03-智慧审查 / 04-法规清理 / "
                "05-实施评估 / 06-意见征集 / 07-立法资料库 / 08-信息门户\n\n"
                "Knife4j UI：http://localhost:8083/api/doc.html  "
                "Swagger UI：http://localhost:8083/api/swagger-ui.html\n"
            ),
            "version": "v0.1.0",
            "contact": {"name": "智立法研发组"},
            "license": {"name": "Proprietary"},
        },
        "servers": [
            {"url": "http://localhost:8083/api",  "description": "本地开发"},
            {"url": "https://dev-api.example.com/api", "description": "测试环境"},
            {"url": "https://api.example.com/api",      "description": "生产环境"},
        ],
        "tags": tags,
        "security": [{"Authorization": []}],
        "paths": paths,
        "components": {
            "securitySchemes": {
                "Authorization": {
                    "type":   "http",
                    "scheme": "bearer",
                    "bearerFormat": "JWT",
                    "description": "登录后获取 token，格式：Bearer {token}",
                },
            },
            "schemas": schemas,
        },
    }

    OUT_FILE.write_text(json.dumps(spec, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"[OK] 生成 {OUT_FILE}  实体 {len(schemas) - 1} 个 + Result，路径 {len(paths)} 条，Tag {len(tags)} 个")


if __name__ == "__main__":
    main()