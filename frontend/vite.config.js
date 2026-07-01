import {defineConfig} from 'vite';
import vue from '@vitejs/plugin-vue';
import path from 'path';

export default defineConfig(({mode}) => {
    return {
        plugins: [vue()],
        resolve: {
            alias: {
                '@': path.resolve(__dirname, './src'),
            },
        },
        optimizeDeps: {
            exclude: ['@ffmpeg/ffmpeg', '@ffmpeg/util'],
        },
        server: {
            proxy: {
                '/ohyesai-next': {
                    target: 'http://127.0.0.1:5173',
                    changeOrigin: true,
                },
            },
            port: 5173,
            open: false,
            host: true,
        },
    };
});
