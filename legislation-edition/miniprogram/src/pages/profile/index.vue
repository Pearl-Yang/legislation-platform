<template>
  <view class="profile-page mobile-content"
    ><view class="profile-card"
      ><view class="profile-avatar"
        ><app-icon name="user-round" :size="48" tone="white" /></view
      ><view class="profile-copy"
        ><view class="profile-name">{{ name }}</view
        ><view class="profile-role">{{ role }}</view
        ><view v-if="department" class="profile-department">{{
          department
        }}</view></view
      ><view class="profile-status">已登录</view></view
    >
    <view class="card"
      ><section-header title="我的工作" /><button
        v-for="item in menus"
        :key="item.title"
        class="profile-menu"
        @click="goPage(item.path)"
      >
        <view class="menu-mark"><app-icon :name="item.icon" :size="36" /></view
        ><view class="menu-copy"
          ><view class="menu-title">{{ item.title }}</view
          ><view class="menu-description">{{ item.description }}</view></view
        ><app-icon
          class="menu-arrow"
          name="chevron-right"
          :size="32"
          tone="muted"
        /></button
    ></view>
    <view class="card"
      ><section-header title="使用与设置" /><button
        class="profile-menu"
        @click="openSettings"
      >
        <view class="menu-copy"
          ><view class="menu-title">连接设置</view
          ><view class="menu-description"
            >配置本地或真机调试服务地址</view
          ></view
        ><text class="menu-arrow">›</text></button
      ><button class="profile-menu" @click="showHelp = true">
        <view class="menu-copy"
          ><view class="menu-title">使用帮助</view
          ><view class="menu-description">工作流程与常见问题</view></view
        ><text class="menu-arrow">›</text>
      </button></view
    >
    <button class="logout-button" @click="logout">退出当前账号</button
    ><view class="profile-footer">智立法 · 移动工作台</view>
    <bottom-sheet v-model="showSettings" title="连接设置"
      ><text class="field-label">后端服务地址</text
      ><input
        v-model="serviceUrl"
        class="field-input"
        placeholder="http://电脑IP:8083/api"
      /><view class="field-hint"
        >真机和电脑需在同一网络。修改服务地址后需要重新登录。</view
      ><button class="primary-button" @click="saveSettings">
        保存并重新登录
      </button></bottom-sheet
    >
    <bottom-sheet v-model="showHelp" title="使用帮助"
      ><view v-for="item in help" :key="item.title" class="help-item"
        ><view class="help-title">{{ item.title }}</view
        ><view class="field-hint">{{ item.description }}</view></view
      ></bottom-sheet
    >
  </view>
</template>
<script>
import { normalizeApiUrl } from "@/utils/connection.js";
import BottomSheet from "@/components/BottomSheet.vue";
import SectionHeader from "@/components/SectionHeader.vue";
import { goPage } from "@/utils/index.js";
import { getBaseUrl } from "@/api/request.js";
import { clearSession, roleLabel } from "@/utils/session.js";
export default {
  components: { BottomSheet, SectionHeader },
  data: () => ({
    name: "",
    role: "",
    department: "",
    showSettings: false,
    showHelp: false,
    serviceUrl: "",
    menus: [
      {
        title: "立法项目",
        description: "项目进度与阶段详情",
        icon: "scale",
        path: "/pages/project/index",
      },
      {
        title: "草案与审查",
        description: "草案版本及审查入口",
        icon: "file-pen-line",
        path: "/pages/draft/index",
      },
      {
        title: "意见征集",
        description: "公开活动与公众建议",
        icon: "messages-square",
        path: "/pages/consultation/index",
      },
      {
        title: "资料收藏",
        description: "查看收藏的法规与资料",
        icon: "library",
        path: "/pages/library/index?favorites=1",
      },
    ],
    help: [
      {
        title: "如何跟踪项目？",
        description: "在立法项目中选择项目，查看流程节点、当前阶段与期限提醒。",
      },
      {
        title: "如何提交公众意见？",
        description:
          "打开意见征集活动，阅读草案后填写建议。提交成功后可在活动详情查看意见记录。",
      },
      {
        title: "如何收藏资料？",
        description:
          "在资料库打开资料，点击收藏；个人中心的资料收藏可再次访问。",
      },
      {
        title: "加载失败怎么办？",
        description:
          "先检查网络与后端是否启动，再在连接设置确认服务地址，回到页面点击重新加载。",
      },
    ],
  }),
  computed: {
    avatar() {
      return (this.name || "智").slice(0, 1);
    },
  },
  onShow() {
    this.name = uni.getStorageSync("userName") || "立法工作者";
    this.role = roleLabel(uni.getStorageSync("userRole"));
    this.department = uni.getStorageSync("userDept") || "";
  },
  methods: {
    goPage,
    openSettings() {
      this.serviceUrl = getBaseUrl();
      this.showSettings = true;
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
      clearSession();
      uni.reLaunch({ url: "/pages/index/index" });
    },
    logout() {
      uni.showModal({
        title: "退出登录",
        content: "确认退出当前账号？",
        success: ({ confirm }) => {
          if (confirm) {
            clearSession();
            uni.reLaunch({ url: "/pages/index/index" });
          }
        },
      });
    },
  },
};
</script>
<style scoped>
.profile-page {
  padding: 32rpx 24rpx 48rpx;
}
.profile-card {
  display: flex;
  align-items: center;
  gap: 24rpx;
  border-radius: 24rpx;
  padding: 32rpx;
  margin-bottom: 24rpx;
  background: #e8f0fa;
  border: 1rpx solid #dce5ef;
}
.profile-avatar {
  width: 96rpx;
  height: 96rpx;
  line-height: 96rpx;
  border-radius: 24rpx;
  text-align: center;
  background: #235a91;
  color: white;
  font-size: 40rpx;
  flex-shrink: 0;
}
.profile-copy {
  flex: 1;
  min-width: 0;
}
.profile-name {
  font-size: 36rpx;
  font-weight: 650;
  color: #163b64;
}
.profile-role,
.profile-department {
  font-size: 26rpx;
  line-height: 1.6;
  color: #536b87;
  margin-top: 8rpx;
}
.profile-status {
  color: #356743;
  background: #dfede2;
  font-size: 24rpx;
  padding: 8rpx 12rpx;
  border-radius: 8rpx;
  flex-shrink: 0;
}
.profile-menu {
  display: flex;
  align-items: center;
  gap: 20rpx;
  padding: 24rpx 0;
  margin: 0;
  background: transparent;
  text-align: left;
  line-height: 1.5;
  border-bottom: 1rpx solid #e7edf4;
}
.profile-menu:last-child {
  border: 0;
}
.profile-menu::after {
  border: 0;
}
.menu-mark {
  width: 64rpx;
  height: 64rpx;
  line-height: 64rpx;
  background: #edf3fa;
  border-radius: 16rpx;
  text-align: center;
  color: #235a91;
  font-size: 28rpx;
  flex-shrink: 0;
}
.menu-copy {
  flex: 1;
  min-width: 0;
}
.menu-title {
  font-size: 30rpx;
  color: #23364d;
}
.menu-description {
  font-size: 24rpx;
  color: #617085;
  margin-top: 8rpx;
}
.menu-arrow {
  color: #6b809a;
  font-size: 38rpx;
}
.logout-button {
  background: white;
  border: 1rpx solid #dce5ef;
  color: #a44342;
  border-radius: 16rpx;
  min-height: 92rpx;
  line-height: 92rpx;
  font-size: 30rpx;
}
.logout-button::after {
  border: none;
}
.profile-footer {
  font-size: 24rpx;
  color: #617085;
  text-align: center;
  margin-top: 28rpx;
}
.help-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #23364d;
}
.help-item {
  margin-bottom: 28rpx;
}
.profile-avatar,
.menu-mark {
  display: flex;
  align-items: center;
  justify-content: center;
}
@media (min-width: 960px) {
  .profile-page {
    max-width: 800px;
    padding: 32px;
  }
}
</style>
