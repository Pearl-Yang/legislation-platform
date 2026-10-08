<template>
  <view class="entry-page">
    <view class="entry-brand"
      ><view class="brand-mark"
        ><app-icon name="scale" :size="42" tone="white" /></view
      ><text>智立法</text><text class="brand-caption">移动工作台</text></view
    >
    <view class="entry-intro"
      ><text class="eyebrow">立法工作，随时协同</text
      ><view class="entry-title">让每一步立法<br />都有据可循</view
      ><view class="entry-description"
        >项目进度、草案审查与公众意见， 在手机上清晰呈现。</view
      ></view
    >
    <view class="entry-flow"
      ><view v-for="(item, index) in stages" :key="item" class="flow-item"
        ><text class="flow-number">0{{ index + 1 }}</text
        ><text>{{ item }}</text></view
      ></view
    >
    <view class="login-card"
      ><section-header
        :title="loggedIn ? '欢迎回来' : '账号登录'"
        description="使用平台账号进入移动工作台"
      />
      <view v-if="!loggedIn"
        ><text class="field-label">账号</text
        ><input
          v-model="form.username"
          class="field-input"
          placeholder="请输入用户名"
          autocomplete="username"
          :maxlength="64"
        />
        <text class="field-label">密码</text
        ><input
          v-model="form.password"
          class="field-input"
          password
          placeholder="请输入密码"
          :maxlength="128"
          @confirm="login"
        />
        <text v-if="error" class="inline-error">{{ error }}</text
        ><button
          class="primary-button"
          :loading="submitting"
          :disabled="submitting"
          @click="login"
        >
          {{ submitting ? "正在登录" : "登录并进入工作台" }}
        </button>
        <view class="login-note">本地测试账号：admin · 密码：123456</view></view
      >
      <view v-else
        ><text class="welcome-name">{{ userName }}</text
        ><button class="primary-button" @click="enterApp">进入工作台</button
        ><button class="text-button" @click="switchAccount">
          切换账号
        </button></view
      >
      <button class="text-button" @click="showSettings = true">
        连接设置
      </button> </view
    ><view class="entry-footer">智立法 · 行政立法智能辅助平台</view>
    <bottom-sheet v-model="showSettings" title="连接设置"
      ><text class="field-label">服务地址</text
      ><input
        v-model="serviceUrl"
        class="field-input"
        placeholder="http://电脑局域网IP:8083/api"
      /><view class="field-hint"
        >电脑预览可使用默认地址。真机调试请填写电脑的局域网地址。</view
      ><button class="primary-button" @click="saveSettings">
        保存地址
      </button></bottom-sheet
    >
  </view>
</template>
<script>
import { normalizeApiUrl } from "@/utils/connection.js";
import SectionHeader from "@/components/SectionHeader.vue";
import BottomSheet from "@/components/BottomSheet.vue";
import { authApi } from "@/api/index.js";
import { getBaseUrl } from "@/api/request.js";
import { saveSession, clearSession, isLoggedIn } from "@/utils/session.js";
export default {
  components: { SectionHeader, BottomSheet },
  data: () => ({
    stages: ["立项", "起草", "审查", "公布", "清理", "评估"],
    form: { username: "admin", password: "" },
    loggedIn: false,
    userName: "",
    error: "",
    submitting: false,
    showSettings: false,
    serviceUrl: "",
  }),
  onShow() {
    this.loggedIn = isLoggedIn();
    this.userName = uni.getStorageSync("userName");
    this.serviceUrl = getBaseUrl();
  },
  methods: {
    enterApp() {
      uni.switchTab({ url: "/pages/dashboard/index" });
    },
    switchAccount() {
      clearSession();
      this.loggedIn = false;
      this.form.password = "";
    },
    saveSettings() {
      let value;
      try {
        value = normalizeApiUrl(this.serviceUrl);
      } catch (error) {
        uni.showToast({ title: error.message, icon: "none" });
        return;
      }
      uni.setStorageSync("apiBaseUrl", value);
      this.showSettings = false;
      uni.showToast({ title: "地址已保存", icon: "success" });
    },
    async login() {
      if (this.submitting) return;
      this.error = "";
      if (!this.form.username.trim() || !this.form.password) {
        this.error = "请填写账号和密码";
        return;
      }
      this.submitting = true;
      try {
        const data = await authApi.login(
          this.form.username.trim(),
          this.form.password,
        );
        saveSession(data);
        this.form.password = "";
        this.loggedIn = true;
        this.enterApp();
      } catch (error) {
        this.error = error.message || "登录失败，请重试";
      } finally {
        this.submitting = false;
      }
    },
  },
};
</script>
<style scoped>
.entry-page {
  min-height: 100vh;
  background: #edf3fa;
  padding: calc(48rpx + env(safe-area-inset-top)) 40rpx 40rpx;
  max-width: 640px;
  margin: auto;
  box-sizing: border-box;
}
.entry-brand {
  display: flex;
  align-items: center;
  gap: 16rpx;
  font-size: 34rpx;
  font-weight: 650;
  color: #163b64;
}
.brand-mark {
  width: 72rpx;
  height: 72rpx;
  background: #235a91;
  border-radius: 20rpx;
  color: white;
  text-align: center;
  line-height: 72rpx;
  font-size: 36rpx;
}
.brand-caption {
  font-size: 24rpx;
  color: #617085;
  font-weight: 400;
  margin-left: auto;
}
.entry-intro {
  padding: 56rpx 0 28rpx;
}
.eyebrow {
  font-size: 24rpx;
  color: #536b87;
  letter-spacing: 3rpx;
}
.entry-title {
  font-size: 52rpx;
  line-height: 1.35;
  color: #163b64;
  font-weight: 700;
  margin: 18rpx 0;
}
.entry-description {
  font-size: 28rpx;
  line-height: 1.8;
  color: #526277;
  white-space: pre-line;
}
.entry-flow {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  padding: 24rpx 0 40rpx;
  gap: 8rpx;
}
.flow-item {
  display: flex;
  flex-direction: column;
  gap: 12rpx;
  color: #3e5671;
  font-size: 26rpx;
}
.flow-number {
  font-size: 22rpx;
  color: #647c97;
  border-top: 3rpx solid #bdcede;
  padding-top: 12rpx;
}
.login-card {
  background: white;
  border: 1rpx solid #dce5ef;
  border-radius: 24rpx;
  padding: 32rpx;
  box-shadow: 0 12rpx 32rpx rgba(22, 59, 100, 0.05);
}
.entry-footer {
  text-align: center;
  font-size: 24rpx;
  color: #607087;
  margin-top: 32rpx;
}
.welcome-name {
  font-size: 32rpx;
  color: #23364d;
}
.brand-mark {
  display: flex;
  align-items: center;
  justify-content: center;
}
@media (min-width: 960px) {
  .entry-page {
    max-width: 1120px;
    display: grid;
    grid-template-columns: 1.2fr 1fr;
    grid-template-rows: auto auto 1fr auto;
    gap: 0 72px;
    padding: 72px 56px;
    background: #f2f6fb;
    align-content: center;
  }
  .entry-brand {
    grid-column: 1 / -1;
    margin-bottom: 64px;
  }
  .brand-caption {
    margin-left: 16px;
  }
  .entry-intro {
    grid-column: 1;
    grid-row: 2;
    padding: 0;
    align-self: center;
  }
  .entry-title {
    font-size: 40px;
    line-height: 1.4;
    margin: 20px 0;
  }
  .entry-flow {
    grid-column: 1;
    grid-row: 3;
    padding-top: 40px;
    align-self: start;
  }
  .login-card {
    grid-column: 2;
    grid-row: 2 / 4;
    padding: 32px;
    align-self: center;
  }
  .entry-footer {
    grid-column: 1 / -1;
    margin-top: 72px;
  }
}
</style>
