<template>
  <div class="page-container">
    <div class="page-header">
      <span class="title">法规主表</span>
      <span class="subtitle">集中存储行政法规、部门规章、地方政府规章全文</span>
    </div>

    <el-card>
      <el-form inline>
        <el-form-item label="关键词">
          <el-input v-model="filter.keyword" placeholder="支持模糊搜索" clearable style="width: 240px" />
        </el-form-item>
        <el-form-item label="法规类型">
          <el-select v-model="filter.regulationType" placeholder="全部" clearable style="width: 180px">
            <el-option v-for="o in projectTypeOptions" :key="o.value" :value="o.value" :label="o.label" />
          </el-select>
        </el-form-item>
        <el-form-item label="效力状态">
          <el-select v-model="filter.status" placeholder="全部" clearable style="width: 140px">
            <el-option value="EFFECTIVE" label="现行有效" />
            <el-option value="REVISING"  label="修订中" />
            <el-option value="OBSOLETE"  label="已废止" />
          </el-select>
        </el-form-item>
        <el-form-item label="地区">
          <el-select v-model="filter.regionCode" placeholder="全部" clearable style="width: 160px">
            <el-option v-for="r in regions" :key="r.value" :value="r.value" :label="r.label" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search">查询</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="mt-16">
      <el-table :data="list" stripe>
        <el-table-column label="法规名称" min-width="280" prop="regulationName">
          <template #default="{ row }">
            <a @click="onOpen(row)">{{ row.regulationName }}</a>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="120">
          <template #default="{ row }">
            <span class="type-badge" :class="projectTypeCls(row.regulationType)">{{ projectTypeLabel(row.regulationType) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="发布机关" prop="issuingAuthority" min-width="160" />
        <el-table-column label="发文字号" prop="issueNumber" min-width="160" />
        <el-table-column label="生效日期" prop="effectiveDate" width="120" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="regStatusTag(row.status)" size="small">{{ regStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="数据来源" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="row.isFromCrawler ? 'primary' : 'success'" effect="plain">
              {{ row.isFromCrawler ? '爬虫' : '人工' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="onOpen(row)">详情</el-button>
            <el-button link size="small" @click="onGraph(row)">关系图</el-button>
            <el-button link size="small" @click="onImpact(row)">影响分析</el-button>
            <el-button link type="danger" size="small" @click="onObsolete(row)">标废止</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listRegulations } from '@/api/legislation'
import { projectTypeOptions, projectTypeLabel, projectTypeCls, regStatusLabel, regStatusTag } from '@/utils/dict'

const filter = ref({ keyword: '', regulationType: '', status: '', regionCode: '' })
const regions = [
  { value: '000000', label: '全国'   },
  { value: '110000', label: '北京'   },
  { value: '310000', label: '上海'   },
  { value: '320000', label: '江苏'   },
  { value: '330000', label: '浙江'   }
]

const list = ref([
  { id: 1, regulationName: '中华人民共和国数据安全法', regulationType: 'ADMIN_REGULATION', issuingAuthority: '全国人大常委会', issueNumber: '主席令第 84 号', effectiveDate: '2021-09-01', status: 'EFFECTIVE', isFromCrawler: true  },
  { id: 2, regulationName: '中华人民共和国网络安全法', regulationType: 'ADMIN_REGULATION', issuingAuthority: '全国人大常委会', issueNumber: '主席令第 53 号', effectiveDate: '2017-06-01', status: 'EFFECTIVE', isFromCrawler: true  },
  { id: 3, regulationName: '某省数据交易管理办法',     regulationType: 'LOCAL_RULE',       issuingAuthority: '某省人民政府',     issueNumber: '省政府令第 318 号', effectiveDate: '2023-03-01', status: 'EFFECTIVE', isFromCrawler: false },
  { id: 4, regulationName: '某市网络数据管理细则',     regulationType: 'LOCAL_RULE',       issuingAuthority: '某市人民政府',     issueNumber: '市政府令第 56 号',  effectiveDate: '2022-08-15', status: 'OBSOLETE',  isFromCrawler: true  },
  { id: 5, regulationName: 'XX 部门信息安全规范',     regulationType: 'DEPT_RULE',        issuingAuthority: '工业和信息化部',   issueNumber: '工信部第 32 号',     effectiveDate: '2018-01-01', status: 'REVISING',  isFromCrawler: true  }
])

const onOpen = (row) => ElMessage.info(`打开法规详情：${row.regulationName}`)
const onGraph = (row) => ElMessage.info(`查看关系图：${row.regulationName}`)
const onImpact = async (row) => {
  await ElMessageBox.alert(`本法规变更将影响 12 部下位法，共 23 个条款需配合修订。`, '影响分析', { type: 'info' })
}
const onObsolete = async (row) => {
  await ElMessageBox.confirm(`确认将「${row.regulationName}」标记为已废止？`, '标废止', { type: 'warning' })
  row.status = 'OBSOLETE'
  ElMessage.success('已标记为已废止')
}
</script>