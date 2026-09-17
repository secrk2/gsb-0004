<template>
  <div class="app-shell">
    <header v-if="session.user" class="topbar">
      <div class="brand" @click="goHome">
        <span class="logo">矫</span>
        <div>
          <div class="title">矫务通</div>
          <div class="subtitle">清河区社区矫正管理平台</div>
        </div>
      </div>

      <nav v-if="isStaff()" class="nav">
        <RouterLink to="/dashboard">矫务作战台</RouterLink>
        <RouterLink to="/objects">对象档案</RouterLink>
      </nav>

      <div class="who">
        <span class="who-name">{{ session.user.displayName }}</span>
        <span class="badge s-ACTIVE">{{ session.user.roleLabel }}</span>
        <span v-if="session.user.officeName" class="muted small office-name">{{ session.user.officeName }}</span>
        <button class="subtle" @click="doLogout">退出</button>
      </div>
    </header>

    <main class="page" :class="{ 'no-topbar': !session.user }">
      <RouterView />
    </main>

    <div class="toast-wrap">
      <div v-for="t in toasts" :key="t.id" class="toast" :class="t.type">{{ t.message }}</div>
    </div>
  </div>
</template>

<script setup>
import { RouterLink, RouterView, useRouter } from 'vue-router'
import { isStaff, logout, session, toasts } from './store.js'
import { api } from './api.js'

const router = useRouter()

function goHome() {
  router.push(isStaff() ? '/dashboard' : '/mobile')
}

async function doLogout() {
  try { await api('/api/auth/logout', { method: 'POST' }) } catch { /* ignore */ }
  logout()
  router.push('/login')
}
</script>

<style scoped>
.app-shell { min-height: 100%; display: flex; flex-direction: column; }

.topbar {
  height: 60px;
  background: linear-gradient(100deg, var(--brand-dark), var(--brand));
  color: #fff;
  display: flex;
  align-items: center;
  padding: 0 22px;
  gap: 26px;
  box-shadow: 0 2px 12px rgba(20, 58, 102, .25);
  position: sticky;
  top: 0;
  z-index: 100;
}
.brand { display: flex; align-items: center; gap: 10px; cursor: pointer; }
.logo {
  width: 38px; height: 38px; border-radius: 9px;
  background: rgba(255,255,255,.16);
  display: flex; align-items: center; justify-content: center;
  font-size: 20px; font-weight: 700;
}
.title { font-size: 17px; font-weight: 700; letter-spacing: 1px; }
.subtitle { font-size: 11px; opacity: .75; }

.nav { display: flex; gap: 4px; margin-left: 10px; }
.nav a {
  color: rgba(255,255,255,.82);
  padding: 7px 16px;
  border-radius: 7px;
  font-size: 14px;
}
.nav a:hover { background: rgba(255,255,255,.12); color: #fff; }
.nav a.router-link-active { background: rgba(255,255,255,.2); color: #fff; font-weight: 600; }

.who { margin-left: auto; display: flex; align-items: center; gap: 10px; }
.who-name { font-size: 14px; }
.who .badge { background: rgba(255,255,255,.2); color: #fff; }
.office-name { color: rgba(255,255,255,.7) !important; }
.who button { background: rgba(255,255,255,.15); color: #fff; padding: 6px 14px; }
.who button:hover { background: rgba(255,255,255,.28); }

.page { flex: 1; padding: 20px 24px 40px; max-width: 1500px; width: 100%; margin: 0 auto; }
.page.no-topbar { padding: 0; max-width: none; }

@media (max-width: 760px) {
  .topbar { padding: 0 12px; gap: 10px; height: 54px; }
  .subtitle, .office-name { display: none; }
  .nav a { padding: 6px 10px; font-size: 13px; }
  .who { gap: 6px; }
  .who-name { max-width: 72px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
  .page { padding: 14px 12px 32px; }
}
</style>
