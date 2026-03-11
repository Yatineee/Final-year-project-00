import path from 'path';
import { fileURLToPath } from 'url';
import CopyPlugin from 'copy-webpack-plugin';

const __dirname = path.dirname(fileURLToPath(import.meta.url));

export default {
  mode: 'production',
  // 1. Entry points for both parts of your extension
  entry: {
    popup: './src/popup/popup.js',
    dashboard: './src/popup/dashboard.js',
    content: './src/popup/content.js',
    background: './src/popup/background.js'
  },
  output: {
    // 2. This will create popup.bundle.js and content.bundle.js
    filename: '[name].bundle.js',
    path: path.resolve(__dirname, 'dist'),
    clean: true, 
  },
  plugins: [
    // 3. Automatically moves these files into /dist
    new CopyPlugin({
      patterns: [
        { from: "manifest.json", to: "manifest.json" },
        { from: "src/popup/popup.html", to: "popup.html" },
        { from: "src/popup/styles.css", to: "styles.css" },
        { from: "src/popup/dashboard.html", to: "dashboard.html" },
        { from: "src/popup/icons", to: "icons" },
        { from: "logo.png", to: "logo.png" },
      ],
    }),
  ],
  module: {
    rules: [
      {
        test: /\.js$/,
        type: 'javascript/auto',
        resolve: { fullySpecified: false }
      }
    ]
  }
};