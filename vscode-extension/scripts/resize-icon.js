/**
 * 将 images/icon.png 缩放到 128x128（Marketplace 要求）
 */
const fs = require('fs');
const path = require('path');

const iconPath = path.join(__dirname, '..', 'images', 'icon.png');
const size = 128;

async function run() {
  let sharp;
  try {
    sharp = require('sharp');
  } catch (e) {
    if (e.code === 'MODULE_NOT_FOUND') {
      console.warn('Run: npm install --save-dev sharp');
      console.warn('Or manually resize images/icon.png to 128x128 pixels and republish.');
      process.exit(1);
    }
    throw e;
  }
  await sharp(iconPath)
    .resize(size, size)
    .toFile(iconPath + '.tmp');
  fs.renameSync(iconPath + '.tmp', iconPath);
  console.log('Icon resized to 128x128:', iconPath);
}
run().catch(err => {
  console.error('Resize failed:', err.message);
  process.exit(1);
});
