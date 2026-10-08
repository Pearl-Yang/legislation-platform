<template>
  <image
    class="app-icon"
    :src="source"
    :style="{ width: size + 'rpx', height: size + 'rpx' }"
    mode="aspectFit"
    aria-hidden="true"
  />
</template>
<script>
export default {
  props: {
    name: { type: String, default: "inbox" },
    size: { type: [Number, String], default: 36 },
    tone: { type: String, default: "accent" },
  },
  computed: {
    source() {
      let extension = "png";
      // #ifdef H5
      extension = "svg";
      // #endif
      return "/static/icons/" + this.name + "-" + this.tone + "." + extension;
    },
  },
};
</script>
<style scoped>
.app-icon {
  display: inline-block;
  flex-shrink: 0;
  vertical-align: middle;
}
</style>
