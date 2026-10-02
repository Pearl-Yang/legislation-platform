<template>
  <div class="page-container">
    <div class="page-header">
      <span class="title">公众意见参与</span>
      <span class="subtitle">匿名/实名均可提交，提交后将进入 AI 归类与人工审议流程</span>
    </div>

    <el-row :gutter="16">
      <el-col :xs="24" :md="14">
        <el-card>
          <template #header>
            <span class="title">提交意见</span>
          </template>
          <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
            <el-form-item label="选择征集" prop="consultationId">
              <el-select v-model="form.consultationId" placeholder="请选择" filterable style="width: 100%" @change="onSelectConsult">
                <el-option v-for="c in openConsultations" :key="c.id" :value="c.id" :label="c.title" />
              </el-select>
            </el-form-item>

            <el-form-item label="匿名提交">
              <el-switch v-model="form.isAnonymous" />
            </el-form-item>

            <template v-if="!form.isAnonymous">
              <el-form-item label="姓名" prop="submitterName">
                <el-input v-model="form.submitterName" />
              </el-form-item>
              <el-form-item label="身份类型">
                <el-select v-model="form.submitterType" placeholder="请选择" style="width: 100%">
                  <el-option value="自然人" label="自然人" />
                  <el-option value="法人"   label="法人（企业）" />
                  <el-option value="社会组织" label="社会组织" />
                </el-select>
              </el-form-item>
              <el-form-item label="联系方式" prop="submitterContact">
                <el-input v-model="form.submitterContact" placeholder="邮箱或手机号" />
              </el-form-item>
            </template>

            <el-form-item label="意见类别">
              <el-select v-model="form.viewpoint" placeholder="请选择立场" style="width: 100%">
                <el-option value="支持" label="支持" />
                <el-option value="反对" label="反对" />
                <el-option value="中立" label="中立" />
              </el-select>
            </el-form-item>

            <el-form-item label="意见正文" prop="content">
              <el-input v-model="form.content" type="textarea" :rows="8" placeholder="请详细说明您对法规草案的意见或建议，字数 50-2000 字" show-word-limit maxlength="2000" />
            </el-form-item>

            <el-form-item label="附件">
              <el-upload action="#" :auto-upload="false" multiple :limit="3">
                <el-button :icon="Upload">点击上传</el-button>
                <template #tip>
                  <div class="el-upload__tip">支持 PDF / Word / 图片，单文件不超过 10MB，最多 3 个</div>
                </template>
              </el-upload>
            </el-form-item>

            <el-form-item>
              <el-checkbox v-model="form.agreePublic">同意将本意见摘要向社会公开</el-checkbox>
            </el-form-item>

            <el-button type="primary" :loading="submitting" :icon="Promotion" @click="onSubmit">提交意见</el-button>
            <el-button @click="onReset">清空</el-button>
          </el-form>
        </el-card>
      </el-col>

      <el-col :xs="24" :md="10">
        <el-card>
          <template #header>
            <span class="title">我的提交记录</span>
          </template>
          <div class="my-list">
            <div v-for="m in mySubmissions" :key="m.id" class="my-row">
              <div class="my-title">{{ m.title }}</div>
              <div class="my-content">{{ m.preview }}</div>
              <div class="my-meta">
                <el-tag size="small" :type="myStatusTag(m.status)">{{ m.statusLabel }}</el-tag>
                <span class="text-secondary">{{ m.time }}</span>
              </div>
            </div>
            <el-empty v-if="!mySubmissions.length" description="暂无提交记录" :image-size="60" />
          </div>
        </el-card>

        <el-card class="mt-16">
          <template #header>
            <span class="title">提交须知</span>
          </template>
          <ul class="tips">
            <li>提交意见前请认真阅读草案文本，确保意见具体、可操作；</li>
            <li>对于匿名意见，我们仍会进行 AI 归类与人工审议，但不再单独通知本人；</li>
            <li>所有采纳 / 不采纳意见的说明将在「公开反馈」环节公示；</li>
            <li>提交即视为接受《意见征集用户须知》。</li>
          </ul>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { Upload, Promotion } from '@element-plus/icons-vue'
import { submitOpinion } from '@/api/legislation'

const formRef = ref()
const submitting = ref(false)

const openConsultations = ref([
  { id: 1, title: '关于《网络数据安全管理条例（草案）》公开征求意见' },
  { id: 2, title: '关于《某省医疗保障办法》公开征求意见' }
])

const form = reactive({
  consultationId: '',
  isAnonymous: false,
  submitterName: '',
  submitterType: '自然人',
  submitterContact: '',
  viewpoint: '',
  content: '',
  agreePublic: true
})

const rules = {
  consultationId: [{ required: true, message: '请选择征集公告', trigger: 'change' }],
  content: [
    { required: true, message: '请输入意见正文', trigger: 'blur' },
    { min: 20, message: '意见至少 20 字', trigger: 'blur' }
  ],
  submitterName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  submitterContact: [{ required: true, message: '请输入联系方式', trigger: 'blur' }]
}

const mySubmissions = ref([
  { id: 11, title: '《网络数据安全管理条例》', preview: '建议第二十条增加对中小企业的差异化条款 ...', status: 'NEW', statusLabel: '已受理', time: '2026-09-29 14:32' },
  { id: 12, title: '《某省医疗保障办法》',     preview: '建议扩大门诊报销范围 ...', status: 'REPLIED', statusLabel: '已回复', time: '2026-09-12 10:11' }
])
const myStatusTag = (s) => ({ NEW: 'warning', PROCESSED: 'primary', REPLIED: 'success' }[s] || 'info')

const onSelectConsult = (id) => ElMessage.info(`已选择征集：${id}`)

const onSubmit = async () => {
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      await submitOpinion(form.consultationId, form)
      ElMessage.success('意见提交成功！已进入归类与审议环节')
      onReset()
    } finally {
      submitting.value = false
    }
  })
}

const onReset = () => {
  formRef.value?.resetFields()
}
</script>

<style lang="scss" scoped>
.tips { padding-left: 18px; margin: 0; }
.tips li { padding: 4px 0; font-size: 13px; color: $text-regular; line-height: 1.7; }

.my-list { display: flex; flex-direction: column; gap: 8px; }
.my-row { padding: 10px 12px; border: 1px solid $border-light; border-radius: 6px; background: $bg-page; }
.my-title { font-size: 13px; font-weight: 500; }
.my-content { font-size: 12px; color: $text-secondary; margin-top: 4px; line-height: 1.6; }
.my-meta { display: flex; gap: 8px; align-items: center; margin-top: 6px; }
.mt-16 { margin-top: 16px; }
</style>