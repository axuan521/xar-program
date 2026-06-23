<template>
  <div class="tech-home">
    <!-- 粒子背景画布 -->
    <canvas ref="particleCanvas" class="particle-bg"></canvas>

    <!-- 顶部英雄区 -->
    <section class="hero-section">
      <div class="hero-content">
        <div class="badge">
          <span class="pulse-dot"></span>
          系统运行中
        </div>
        <h1 class="glitch-title">xar后台管理系统</h1>
        <p class="subtitle">
          <span class="typing-text">{{ typedText }}</span>
          <span class="cursor">|</span>
        </p>
        <div class="tech-tags">
          <span class="tag" v-for="(tag, index) in techTags" :key="index">
            <i :class="tag.icon"></i>
            {{ tag.name }}
          </span>
        </div>
        <div class="hero-actions">
          <el-button
            type="primary"
            size="medium"
            class="glow-btn"
            @click="goToUsers"
          >
            <i class="el-icon-s-promotion"></i>
            进入系统
          </el-button>
          <el-button size="medium" class="outline-btn" @click="showStats">
            <i class="el-icon-data-line"></i>
            查看数据
          </el-button>
        </div>
      </div>
      <!-- 3D旋转立方体装饰 -->
      <div class="cube-container">
        <div class="cube">
          <div class="face front">API</div>
          <div class="face back">JWT</div>
          <div class="face right">Vue</div>
          <div class="face left">Spring</div>
          <div class="face top">MySQL</div>
          <div class="face bottom">Redis</div>
        </div>
      </div>
    </section>

    <!-- 数据看板 -->
    <section class="stats-section">
      <el-row :gutter="20">
        <el-col :xs="12" :sm="6" v-for="(stat, index) in stats" :key="index">
          <div class="stat-card" :class="stat.color">
            <div class="stat-icon">
              <i :class="stat.icon" style="font-size: 32px"></i>
            </div>
            <div class="stat-info">
              <div class="stat-value">
                {{ stat.value }}
                <span class="unit">{{ stat.unit }}</span>
              </div>
              <div class="stat-label">{{ stat.label }}</div>
            </div>
            <div class="stat-trend" :class="stat.trend > 0 ? 'up' : 'down'">
              <i :class="stat.trend > 0 ? 'el-icon-top' : 'el-icon-bottom'"></i>
              {{ Math.abs(stat.trend) }}%
            </div>
          </div>
        </el-col>
      </el-row>
    </section>

    <!-- 技术架构 -->
    <section class="tech-stack">
      <h2 class="section-title">
        <span class="title-line"></span>
        技术架构
        <span class="title-line"></span>
      </h2>
      <el-row :gutter="24">
        <el-col
          :xs="24"
          :sm="12"
          :md="8"
          v-for="(stack, index) in techStacks"
          :key="index"
        >
          <div
            class="stack-card"
            @mouseenter="hoverCard = stack.name"
            @mouseleave="hoverCard = ''"
          >
            <div
              class="card-glow"
              :class="{ active: hoverCard === stack.name }"
            ></div>
            <div class="stack-header">
              <div class="stack-icon" :style="{ background: stack.gradient }">
                <i :class="stack.icon" style="font-size: 28px; color: #fff"></i>
              </div>
              <h3>{{ stack.name }}</h3>
              <span class="version">{{ stack.version }}</span>
            </div>
            <div class="stack-body">
              <p>{{ stack.desc }}</p>
              <div class="tech-list">
                <span
                  class="tech-item"
                  v-for="(item, idx) in stack.items"
                  :key="idx"
                >
                  {{ item }}
                </span>
              </div>
            </div>
            <div class="stack-footer">
              <div class="progress-bar">
                <div
                  class="progress-fill"
                  :style="{ width: stack.progress + '%' }"
                ></div>
              </div>
              <span class="progress-text">完成度 {{ stack.progress }}%</span>
            </div>
          </div>
        </el-col>
      </el-row>
    </section>

    <!-- 功能特性 -->
    <section class="features">
      <h2 class="section-title">
        <span class="title-line"></span>
        核心功能
        <span class="title-line"></span>
      </h2>
      <div class="feature-grid">
        <div
          class="feature-item"
          v-for="(feature, index) in features"
          :key="index"
          @mouseenter="activeFeature = feature.title"
          @mouseleave="activeFeature = ''"
        >
          <div
            class="feature-icon"
            :class="{ active: activeFeature === feature.title }"
          >
            <i :class="feature.icon" style="font-size: 40px"></i>
          </div>
          <h4>{{ feature.title }}</h4>
          <p>{{ feature.desc }}</p>
          <div class="feature-line"></div>
        </div>
      </div>
    </section>

    <!-- 系统状态 -->
    <section class="system-status">
      <el-card class="status-card" shadow="hover">
        <div slot="header" class="status-header">
          <i class="el-icon-monitor"></i>
          <span>系统状态监控</span>
          <span class="status-badge online">在线</span>
        </div>
        <div class="status-grid">
          <div
            class="status-item"
            v-for="(status, index) in systemStatus"
            :key="index"
          >
            <div class="status-ring">
              <svg viewBox="0 0 100 100">
                <circle class="ring-bg" cx="50" cy="50" r="45" />
                <circle
                  class="ring-fill"
                  cx="50"
                  cy="50"
                  r="45"
                  :style="{
                    strokeDashoffset: 283 - (283 * status.value) / 100,
                  }"
                />
              </svg>
              <div class="ring-value">{{ status.value }}%</div>
            </div>
            <div class="status-name">{{ status.name }}</div>
          </div>
        </div>
      </el-card>
    </section>

    <!-- 底部波浪 -->
    <div class="wave-footer">
      <svg viewBox="0 0 1200 120" preserveAspectRatio="none">
        <path
          d="M0,0V46.29c47.79,22.2,103.59,32.17,158,28,70.36-5.37,136.33-33.31,206.8-37.5C438.64,32.43,512.34,53.67,583,72.05c69.27,18,138.3,24.88,209.4,13.08,36.15-6,69.85-17.84,104.45-29.34C989.49,25,1113-14.29,1200,52.47V0Z"
          opacity=".25"
          fill="#409EFF"
        ></path>
        <path
          d="M0,0V15.81C13,36.92,27.64,56.86,47.69,72.05,99.41,111.27,165,111,224.58,91.58c31.15-10.15,60.09-26.07,89.67-39.8,40.92-19,84.73-46,130.83-49.67,36.26-2.85,70.9,9.42,98.6,31.56,31.77,25.39,62.32,62,103.63,73,40.44,10.79,81.35-6.69,119.13-24.28s75.16-39,116.92-43.05c59.73-5.85,113.28,22.88,168.9,38.84,30.2,8.66,59,6.17,87.09-7.5,22.43-10.89,48-26.93,60.65-49.24V0Z"
          opacity=".5"
          fill="#409EFF"
        ></path>
        <path
          d="M0,0V5.63C149.93,59,314.09,71.32,475.83,42.57c43-7.64,84.23-20.12,127.61-26.46,59-8.63,112.48,12.24,165.56,35.4C827.93,77.22,886,95.24,951.2,90c86.53-7,172.46-45.71,248.8-84.81V0Z"
          fill="#409EFF"
        ></path>
      </svg>
      <div class="footer-content">
        <p>xar后台管理系统 v{{ version }} | Spring Boot + Vue 全栈架构</p>
        <p class="copyright">© 2026 xar Team. All rights reserved.</p>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: "Index",
  data() {
    return {
      version: "3.9.2",
      typedText: "",
      fullText: "基于 Spring Boot + Vue 的企业级后台管理框架",
      typingIndex: 0,
      techTags: [
        { name: "Spring Boot", icon: "el-icon-cpu" },
        { name: "Vue", icon: "el-icon-share" },
        { name: "JWT认证", icon: "el-icon-lock" },
        { name: "MySQL", icon: "el-icon-coin" },
        { name: "Element UI", icon: "el-icon-s-grid" },
      ],
      stats: [
        {
          key: "users",
          label: "注册用户",
          value: 1286,
          unit: "人",
          trend: 12.5,
          icon: "el-icon-user",
          color: "blue",
        },
        {
          key: "api",
          label: "API接口",
          value: 86,
          unit: "个",
          trend: 8.3,
          icon: "el-icon-share",
          color: "green",
        },
        {
          key: "online",
          label: "在线用户",
          value: 42,
          unit: "人",
          trend: -2.1,
          icon: "el-icon-monitor",
          color: "orange",
        },
        {
          key: "uptime",
          label: "运行时长",
          value: 99.9,
          unit: "%",
          trend: 0.5,
          icon: "el-icon-timer",
          color: "purple",
        },
      ],
      techStacks: [
        {
          name: "后端架构",
          version: "Spring Boot 3.2",
          icon: "el-icon-cpu",
          gradient: "linear-gradient(135deg, #667eea 0%, #764ba2 100%)",
          desc: "基于Spring Boot 3.x构建，集成Spring Security、JWT、MyBatis-Plus",
          items: ["RESTful API", "JWT认证", "MyBatis-Plus", "AOP日志"],
          progress: 100,
        },
        {
          name: "前端架构",
          version: "Vue + Element UI",
          icon: "el-icon-share",
          gradient: "linear-gradient(135deg, #f093fb 0%, #f5576c 100%)",
          desc: "采用Vue2 + Element UI组件库，响应式布局设计",
          items: ["Vue2", "Element UI", "Vuex", "Axios"],
          progress: 100,
        },
        {
          name: "数据存储",
          version: "MySQL 8.0",
          icon: "el-icon-coin",
          gradient: "linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)",
          desc: "MySQL关系型数据库，Redis缓存加速，Druid连接池",
          items: ["MySQL", "Redis", "Druid", "连接池"],
          progress: 95,
        },
      ],
      features: [
        {
          title: "用户管理",
          desc: "完整的用户CRUD，支持角色权限分配",
          icon: "el-icon-user",
        },
        {
          title: "权限控制",
          desc: "RBAC模型，细粒度权限控制",
          icon: "el-icon-lock",
        },
        {
          title: "数据监控",
          desc: "实时系统状态监控与告警",
          icon: "el-icon-data-line",
        },
        {
          title: "日志审计",
          desc: "操作日志记录与查询分析",
          icon: "el-icon-document",
        },
        {
          title: "消息通知",
          desc: "系统消息推送与邮件通知",
          icon: "el-icon-bell",
        },
        {
          title: "系统配置",
          desc: "灵活的系统参数配置管理",
          icon: "el-icon-setting",
        },
      ],
      systemStatus: [
        { name: "CPU", value: 32 },
        { name: "内存", value: 58 },
        { name: "磁盘", value: 76 },
        { name: "网络", value: 45 },
      ],
      hoverCard: "",
      activeFeature: "",
    };
  },
  mounted() {
    this.startTyping();
    this.initParticles();
  },
  methods: {
    startTyping() {
      if (this.typingIndex < this.fullText.length) {
        this.typedText += this.fullText.charAt(this.typingIndex);
        this.typingIndex++;
        setTimeout(this.startTyping, 50);
      }
    },
    initParticles() {
      const canvas = this.$refs.particleCanvas;
      const ctx = canvas.getContext("2d");
      canvas.width = window.innerWidth;
      canvas.height = window.innerHeight;

      const particles = [];
      const particleCount = 80;

      for (let i = 0; i < particleCount; i++) {
        particles.push({
          x: Math.random() * canvas.width,
          y: Math.random() * canvas.height,
          vx: (Math.random() - 0.5) * 0.5,
          vy: (Math.random() - 0.5) * 0.5,
          size: Math.random() * 2 + 1,
          opacity: Math.random() * 0.5 + 0.2,
        });
      }

      const animate = () => {
        ctx.clearRect(0, 0, canvas.width, canvas.height);

        particles.forEach((p, i) => {
          p.x += p.vx;
          p.y += p.vy;

          if (p.x < 0 || p.x > canvas.width) p.vx *= -1;
          if (p.y < 0 || p.y > canvas.height) p.vy *= -1;

          ctx.beginPath();
          ctx.arc(p.x, p.y, p.size, 0, Math.PI * 2);
          ctx.fillStyle = `rgba(64, 158, 255, ${p.opacity})`;
          ctx.fill();

          for (let j = i + 1; j < particles.length; j++) {
            const dx = particles[j].x - p.x;
            const dy = particles[j].y - p.y;
            const distance = Math.sqrt(dx * dx + dy * dy);

            if (distance < 150) {
              ctx.beginPath();
              ctx.moveTo(p.x, p.y);
              ctx.lineTo(particles[j].x, particles[j].y);
              ctx.strokeStyle = `rgba(64, 158, 255, ${
                0.1 * (1 - distance / 150)
              })`;
              ctx.stroke();
            }
          }
        });

        requestAnimationFrame(animate);
      };

      animate();

      window.addEventListener("resize", () => {
        canvas.width = window.innerWidth;
        canvas.height = window.innerHeight;
      });
    },
    goToUsers() {
      this.$router.push("/system/user");
    },
    showStats() {
      this.$message.success("数据面板加载中...");
    },
  },
};
</script>

<style scoped>
.tech-home {
  min-height: 100vh;
  background: #0a0e27;
  color: #fff;
  position: relative;
  overflow-x: hidden;
}

/* 粒子背景 */
.particle-bg {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 0;
  pointer-events: none;
}

/* 英雄区 */
.hero-section {
  position: relative;
  z-index: 1;
  min-height: 80vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  background: radial-gradient(
    ellipse at center,
    rgba(64, 158, 255, 0.1) 0%,
    transparent 70%
  );
}

.hero-content {
  text-align: center;
  max-width: 800px;
}

.badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background: rgba(64, 158, 255, 0.15);
  border: 1px solid rgba(64, 158, 255, 0.3);
  padding: 8px 20px;
  border-radius: 20px;
  font-size: 14px;
  color: #409eff;
  margin-bottom: 30px;
}

.pulse-dot {
  width: 8px;
  height: 8px;
  background: #67c23a;
  border-radius: 50%;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%,
  100% {
    opacity: 1;
    transform: scale(1);
  }
  50% {
    opacity: 0.5;
    transform: scale(1.2);
  }
}

.glitch-title {
  font-size: 56px;
  font-weight: 700;
  margin: 0 0 20px;
  background: linear-gradient(135deg, #fff 0%, #409eff 50%, #fff 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  letter-spacing: 2px;
}

.subtitle {
  font-size: 18px;
  color: #8892b0;
  margin-bottom: 30px;
  min-height: 30px;
}

.typing-text {
  color: #ccd6f6;
}

.cursor {
  color: #409eff;
  animation: blink 1s infinite;
}

@keyframes blink {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0;
  }
}

.tech-tags {
  display: flex;
  justify-content: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 40px;
}

.tag {
  display: flex;
  align-items: center;
  gap: 6px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  padding: 8px 16px;
  border-radius: 8px;
  font-size: 14px;
  color: #a8b2d1;
  transition: all 0.3s;
}

.tag:hover {
  background: rgba(64, 158, 255, 0.15);
  border-color: rgba(64, 158, 255, 0.3);
  color: #409eff;
  transform: translateY(-2px);
}

.hero-actions {
  display: flex;
  justify-content: center;
  gap: 16px;
}

.glow-btn {
  background: linear-gradient(135deg, #409eff 0%, #64b5f6 100%);
  border: none;
  box-shadow: 0 0 20px rgba(64, 158, 255, 0.4);
  transition: all 0.3s;
}

.glow-btn:hover {
  box-shadow: 0 0 30px rgba(64, 158, 255, 0.6);
  transform: translateY(-2px);
}

.outline-btn {
  background: transparent;
  border: 1px solid rgba(255, 255, 255, 0.2);
  color: #ccd6f6;
}

.outline-btn:hover {
  border-color: #409eff;
  color: #409eff;
  background: rgba(64, 158, 255, 0.1);
}

/* 3D立方体 */
.cube-container {
  position: absolute;
  right: 10%;
  top: 50%;
  transform: translateY(-50%);
  perspective: 1000px;
  opacity: 0.3;
}

.cube {
  width: 120px;
  height: 120px;
  position: relative;
  transform-style: preserve-3d;
  animation: rotate 20s infinite linear;
}

@keyframes rotate {
  from {
    transform: rotateX(0) rotateY(0);
  }
  to {
    transform: rotateX(360deg) rotateY(360deg);
  }
}

.face {
  position: absolute;
  width: 120px;
  height: 120px;
  background: rgba(64, 158, 255, 0.1);
  border: 1px solid rgba(64, 158, 255, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  color: #409eff;
  font-weight: bold;
}

.front {
  transform: translateZ(60px);
}
.back {
  transform: translateZ(-60px) rotateY(180deg);
}
.right {
  transform: translateX(60px) rotateY(90deg);
}
.left {
  transform: translateX(-60px) rotateY(-90deg);
}
.top {
  transform: translateY(-60px) rotateX(90deg);
}
.bottom {
  transform: translateY(60px) rotateX(-90deg);
}

/* 数据看板 */
.stats-section {
  position: relative;
  z-index: 1;
  padding: 40px 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.stat-card {
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 16px;
  padding: 24px;
  display: flex;
  align-items: center;
  gap: 16px;
  position: relative;
  overflow: hidden;
  transition: all 0.3s;
}

.stat-card:hover {
  transform: translateY(-5px);
  border-color: rgba(64, 158, 255, 0.3);
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.3);
}

.stat-card::before {
  content: "";
  position: absolute;
  top: 0;
  left: 0;
  width: 4px;
  height: 100%;
}

.stat-card.blue::before {
  background: linear-gradient(to bottom, #409eff, #64b5f6);
}
.stat-card.green::before {
  background: linear-gradient(to bottom, #67c23a, #85ce61);
}
.stat-card.orange::before {
  background: linear-gradient(to bottom, #e6a23c, #ebb563);
}
.stat-card.purple::before {
  background: linear-gradient(to bottom, #9266cc, #a67ee5);
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.05);
  color: #409eff;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #fff;
}

.unit {
  font-size: 14px;
  color: #8892b0;
  margin-left: 4px;
}

.stat-label {
  font-size: 14px;
  color: #8892b0;
  margin-top: 4px;
}

.stat-trend {
  position: absolute;
  top: 16px;
  right: 16px;
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.stat-trend.up {
  color: #67c23a;
}
.stat-trend.down {
  color: #f56c6c;
}

/* 技术架构 */
.tech-stack {
  position: relative;
  z-index: 1;
  padding: 60px 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.section-title {
  text-align: center;
  font-size: 32px;
  font-weight: 700;
  margin-bottom: 50px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 20px;
}

.title-line {
  width: 60px;
  height: 2px;
  background: linear-gradient(to right, transparent, #409eff);
}

.title-line:last-child {
  background: linear-gradient(to left, transparent, #409eff);
}

.stack-card {
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 16px;
  padding: 30px;
  position: relative;
  overflow: hidden;
  transition: all 0.3s;
  height: 100%;
}

.card-glow {
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(
    circle,
    rgba(64, 158, 255, 0.15) 0%,
    transparent 70%
  );
  opacity: 0;
  transition: opacity 0.3s;
}

.card-glow.active {
  opacity: 1;
}

.stack-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.stack-icon {
  width: 50px;
  height: 50px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stack-header h3 {
  font-size: 20px;
  margin: 0;
}

.version {
  margin-left: auto;
  background: rgba(64, 158, 255, 0.15);
  color: #409eff;
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 12px;
}

.stack-body p {
  color: #8892b0;
  font-size: 14px;
  line-height: 1.6;
  margin-bottom: 16px;
}

.tech-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tech-item {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  padding: 6px 12px;
  border-radius: 6px;
  font-size: 13px;
  color: #a8b2d1;
}

.stack-footer {
  margin-top: 20px;
}

.progress-bar {
  height: 6px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 3px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(to right, #409eff, #64b5f6);
  border-radius: 3px;
  transition: width 1s ease;
}

.progress-text {
  font-size: 12px;
  color: #8892b0;
  margin-top: 8px;
  display: block;
}

/* 功能特性 */
.features {
  position: relative;
  z-index: 1;
  padding: 60px 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.feature-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 24px;
}

.feature-item {
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 16px;
  padding: 32px;
  text-align: center;
  position: relative;
  transition: all 0.3s;
}

.feature-item:hover {
  transform: translateY(-5px);
  border-color: rgba(64, 158, 255, 0.3);
}

.feature-icon {
  width: 70px;
  height: 70px;
  border-radius: 50%;
  background: rgba(64, 158, 255, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 20px;
  color: #409eff;
  transition: all 0.3s;
}

.feature-icon.active {
  background: rgba(64, 158, 255, 0.2);
  box-shadow: 0 0 30px rgba(64, 158, 255, 0.3);
  transform: scale(1.1);
}

.feature-item h4 {
  font-size: 18px;
  margin-bottom: 10px;
}

.feature-item p {
  color: #8892b0;
  font-size: 14px;
  line-height: 1.6;
}

.feature-line {
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 0;
  height: 2px;
  background: linear-gradient(to right, #409eff, #64b5f6);
  transition: width 0.3s;
}

.feature-item:hover .feature-line {
  width: 60%;
}

/* 系统状态 */
.system-status {
  position: relative;
  z-index: 1;
  padding: 40px 20px;
  max-width: 800px;
  margin: 0 auto;
}

.status-card {
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.status-header {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 16px;
  font-weight: 600;
}

.status-badge {
  margin-left: auto;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
}

.status-badge.online {
  background: rgba(103, 194, 58, 0.15);
  color: #67c23a;
}

.status-grid {
  display: flex;
  justify-content: space-around;
  padding: 20px 0;
}

.status-item {
  text-align: center;
}

.status-ring {
  width: 100px;
  height: 100px;
  position: relative;
  margin: 0 auto 10px;
}

.status-ring svg {
  transform: rotate(-90deg);
  width: 100px;
  height: 100px;
}

.ring-bg {
  fill: none;
  stroke: rgba(255, 255, 255, 0.05);
  stroke-width: 8;
}

.ring-fill {
  fill: none;
  stroke: #409eff;
  stroke-width: 8;
  stroke-linecap: round;
  stroke-dasharray: 283;
  transition: stroke-dashoffset 1s ease;
}

.ring-value {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-size: 20px;
  font-weight: 700;
  color: #fff;
}

.status-name {
  font-size: 14px;
  color: #8892b0;
}

/* 波浪底部 */
.wave-footer {
  position: relative;
  z-index: 1;
  margin-top: 60px;
}

.wave-footer svg {
  width: 100%;
  height: 100px;
  display: block;
}

.footer-content {
  background: #409eff;
  padding: 30px 20px;
  text-align: center;
}

.footer-content p {
  margin: 0;
  color: rgba(255, 255, 255, 0.9);
  font-size: 14px;
}

.copyright {
  margin-top: 8px !important;
  opacity: 0.7;
}

/* 响应式 */
@media (max-width: 768px) {
  .glitch-title {
    font-size: 32px;
  }

  .cube-container {
    display: none;
  }

  .hero-section {
    min-height: 60vh;
  }

  .feature-grid {
    grid-template-columns: 1fr;
  }
}
</style>
