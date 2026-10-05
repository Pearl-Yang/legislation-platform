<template>
  <div class="page-container project-page">
    <div class="page-header">
      <div>
        <span class="title">立法项目全流程</span>
        <span class="subtitle">依据《行政法规制定程序条例》《规章制定程序条例》配置流程模板</span>
      </div>
      <el-button type="primary" :icon="Plus" @click="dialogVisible = true">新建立法项目</el-button>
    </div>

    <!-- 顶部 4 个状态 tab -->
    <el-tabs v-model="activeStatus" class="status-tabs" @tab-change="onSearch">
      <el-tab-pane label="全部"     name="all" />
      <el-tab-pane label="草稿"     name="DRAFT" />
      <el-tab-pane label="进行中"   name="ACTIVE" />
      <el-tab-pane label="已发布"   name="PUBLISHED" />
      <el-tab-pane label="已废止"   name="OBSOLETE" />
    </el-tabs>

    <!-- 筛选条 -->
    <el-card class="filter-bar">
      <el-form inline>
        <el-form-item label="项目类型">
          <el-select v-model="filter.projectType" placeholder="全部" clearable style="width: 180px" @change="onSearch">
            <el-option v-for="o in projectTypeOptions" :key="o.value" :value="o.value" :label="o.label" />
          </el-select>
        </el-form-item>
        <el-form-item label="项目名称">
          <el-input v-model="filter.keyword" placeholder="支持模糊搜索" clearable style="width: 240px" @keyup.enter="onSearch" @clear="onSearch" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="onSearch">查询</el-button>
          <el-button :icon="Refresh" @click="onReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 列表卡片 -->
    <div class="project-list mt-16" v-loading="loading">
      <el-empty v-if="!loading && list.length === 0" description="暂无项目，点击右上角新建或调整筛选条件" />

      <div v-for="p in list" :key="p.id" class="project-row">
        <div class="pr-left">
          <div class="pr-head">
            <span class="type-badge" :class="projectTypeCls(p.projectType)">{{ projectTypeLabel(p.projectType) }}</span>
            <el-tag :type="projectStatusTag(p.status)" size="small" effect="light">{{ projectStatusLabel(p.status) }}</el-tag>
            <span class="stage-dot" :class="stageStatusCls(p.currentStageStatus)"></span>
            <span class="pr-stage">{{ p.currentStageName || '待立项' }}</span>
          </div>
          <div class="pr-name">
            <a @click="$router.push(`/app/project/detail/${p.id}`)">{{ p.projectName }}</a>
          </div>
          <div class="pr-desc">{{ p.description || '暂无描述' }}</div>
          <div class="pr-meta">
            <span><el-icon><OfficeBuilding /></el-icon>{{ p.issuingAuthority || '未指定' }}</span>
            <span><el-icon><Calendar /></el-icon>{{ p.publishDate || '—' }}</span>
            <span><el-icon><Document /></el-icon>{{ p.documentNumber || '尚未取得文号' }}</span>
          </div>
        </div>

        <div class="pr-progress">
          <div class="pr-prog-text">
            <span>整体进度</span>
            <strong>{{ p.progress || 0 }}%</strong>
          </div>
          <el-progress :percentage="p.progress || 0" :stroke-width="6" :show-text="false" :color="progressColor" />
          <div class="pr-prog-meta">
            <span>{{ p.doneStageCount || 0 }} / {{ p.totalStageCount || 0 }} 节点</span>
          </div>
        </div>

        <div class="pr-actions">
          <el-button type="primary" link @click="$router.push(`/app/project/detail/${p.id}`)">详情</el-button>
          <el-button link @click="$router.push('/app/draft/generate')">生成草案</el-button>
          <el-dropdown trigger="click">
            <el-button link>更多<el-icon class="el-icon--right"><ArrowDown /></el-icon></el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="onAdvance(p)">推进下一阶段</el-dropdown-item>
                <el-dropdown-item @click="onRollback(p)">回退到指定阶段</el-dropdown-item>
                <el-dropdown-item divided @click="onDelete(p)">删除</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>

      <!-- 分页 -->
      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="page.current"
          v-model:page-size="page.size"
          :total="page.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, jumper, prev, pager, next"
          @current-change="onSearch"
          @size-change="onSearch"
        />
      </div>
    </div>

    <!-- 新建弹窗 -->
    <el-dialog v-model="dialogVisible" title="新建立法项目" width="640px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item label="项目名称" prop="projectName">
          <el-input v-model="form.projectName" placeholder="例：网络数据安全管理条例" />
        </el-form-item>
        <el-form-item label="项目类型" prop="projectType">
          <el-select v-model="form.projectType" placeholder="选择项目类型以加载对应流程模板" style="width: 100%">
            <el-option v-for="o in projectTypeOptions" :key="o.value" :value="o.value" :label="o.label" />
          </el-select>
        </el-form-item>
        <el-form-item label="立项依据" prop="legalBasis">
          <el-input v-model="form.legalBasis" placeholder="例：依据《数据安全法》第二十一条 ..." />
        </el-form-item>
        <el-form-item label="协调部门" prop="coordinatingDepartments">
          <el-input v-model="form.coordinatingDepartments" placeholder="涉及多个部门时用逗号分隔" />
        </el-form-item>
        <el-form-item label="优先级" prop="priority">
          <el-radio-group v-model="form.priority">
            <el-radio-button value="HIGH">高</el-radio-button>
            <el-radio-button value="MEDIUM">中</el-radio-button>
            <el-radio-button value="LOW">低</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="项目描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="4" placeholder="说明项目的立法目的、拟解决的主要问题等" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="onCreate">创建并初始化流程</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Refresh, ArrowDown, Calendar, Document, OfficeBuilding } from '@element-plus/icons-vue'
import {
  listProjects, createProject, advanceProject, rollbackProject, deleteProject
} from '@/api/legislation'
import { projectTypeOptions, projectTypeLabel, projectTypeCls,
         projectStatusLabel, projectStatusTag, stageStatusCls } from '@/utils/dict'

const loading = ref(false)
const creating = ref(false)
const dialogVisible = ref(false)
const formRef = ref()
const activeStatus = ref('all')

const filter = reactive({ projectType: '', keyword: '' })
const page = reactive({ current: 1, size: 10, total: 0 })

const list = ref([])

const progressColor = [
  { color: '#f43f5e', percentage: 20 },
  { color: '#f59e0b', percentage: 50 },
  { color: '#10b981', percentage: 80 },
  { color: '#4f46e5', percentage: 100 }
]

const form = reactive({
  projectName: '', projectType: '', description: '',
  legalBasis: '', coordinatingDepartments: '', priority: 'MEDIUM'
})
const formRules = {
  projectName: [{ required: true, message: '请输入项目名称', trigger: 'blur' }],
  projectType:  [{ required: true, message: '请选择项目类型', trigger: 'change' }]
}

const onSearch = async () => {
  loading.value = true
  try {
    const params = {
      page: page.current,
      size: page.size
    }
    if (filter.projectType) params.projectType = filter.projectType
    if (activeStatus.value !== 'all') params.status = activeStatus.value

    const { data } = await listProjects(params)
    // 后端返回 MyBatis-Plus Page 结构 {records, total, current, size}
    const records = data?.records || []
    list.value = records
    page.total = data?.total || 0
  } catch (e) {
    // 错误
  } finally {
    loading.value = false
  }
}

const onReset = () => { filter.projectType = ''; filter.keyword = ''; onSearch() }

const onCreate = async () => {
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    creating.value = true
    try {
      await createProject(form)
      ElMessage.success('已创建并自动初始化流程节点')
      dialogVisible.value = false
      Object.assign(form, { projectName: '', projectType: '', description: '', legalBasis: '', coordinatingDepartments: '', priority: 'MEDIUM' })
      page.current = 1
      onSearch()
    } finally {
      creating.value = false
    }
  })
}

const onAdvance = async (row) => {
  try {
    await advanceProject(row.id, {})
    ElMessage.success('已推进到下一阶段')
    onSearch()
  } catch (e) {}
}

const onRollback = async (row) => {
  try {
    const { value } = await ElMessageBox.prompt('请输入目标阶段顺序号（从 1 开始）', '回退阶段', { inputPattern: /^\d+$/, inputErrorMessage: '请输入正整数' })
    await rollbackProject(row.id, { targetStageOrder: Number(value) })
    ElMessage.success('已回退到指定阶段')
    onSearch()
  } catch (e) {}
}

const onDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确认删除项目「${row.projectName}」？此操作不可恢复`, '确认删除', { type: 'warning' })
    await deleteProject(row.id)
    ElMessage.success('已删除')
    onSearch()
  } catch (e) {}
}

onMounted(onSearch)
</script>

<style lang="scss" scoped>
.status-tabs :deep(.el-tabs__header) { margin-bottom: 12px; }

.filter-bar { margin-bottom: 12px; }

.project-row {
  display: grid;
  grid-template-columns: 1fr 240px 140px;
  gap: 16px;
  background: #fff;
  border-radius: 8px;
  padding: 16px 20px;
  margin-bottom: 12px;
  border: 1px solid $border-light;
  transition: all 0.18s;

  &:hover {
    box-shadow: $shadow-elevated;
    border-color: $primary-lighter;
    transform: translateY(-1px);
  }
  .pr-head { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
  .pr-stage { font-size: 12px; color: $text-secondary; }
  .pr-name { font-size: 16px; font-weight: 600; margin-top: 6px; }
  .pr-name a { color: $text-primary; &:hover { color: $primary-color; } }
  .pr-desc { font-size: 13px; color: $text-secondary; margin-top: 4px; line-height: 1.6; }
  .pr-meta {
    display: flex; gap: 16px; flex-wrap: wrap; margin-top: 8px;
    font-size: 12px; color: $text-secondary;
    span { display: inline-flex; align-items: center; gap: 4px; }
    .el-icon { font-size: 13px; }
  }

  .pr-progress { display: flex; flex-direction: column; justify-content: center; }
  .pr-prog-text { display: flex; justify-content: space-between; font-size: 13px; color: $text-secondary; strong { font-size: 18px; color: $primary-color; } }
  .pr-prog-meta { font-size: 12px; color: $text-secondary; margin-top: 8px; }

  .pr-actions { display: flex; flex-direction: column; align-items: flex-end; justify-content: center; gap: 4px; }
}

.pagination-wrap { display: flex; justify-content: flex-end; margin-top: 16px; }

@media (max-width: 1100px) {
  .project-row { grid-template-columns: 1fr; }
  .pr-actions { flex-direction: row; align-items: center; }
}
</style>