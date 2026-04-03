import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import vueJsx from '@vitejs/plugin-vue-jsx';
import path from 'path';

export default defineConfig({
  plugins: [vue(), vueJsx()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src')
    }
  },
  build: {
    chunkSizeWarningLimit: 900,
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (!id.includes('node_modules')) return;
          if (id.includes('@element-plus/icons-vue')) return 'element-icons';
          if (id.includes('element-plus')) return 'element-plus';
          if (id.includes('axios')) return 'axios';
          if (id.includes('pinia') || id.includes('vue-router')) return 'vue-vendor';
          if (id.includes('node_modules/vue/') || id.includes('node_modules/@vue/')) return 'vue-vendor';
        }
      }
    }
  },
  server: {
    port: 5173,
    open: true,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
});

