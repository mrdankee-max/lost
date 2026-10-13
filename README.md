# Lost BTC — Android starter

## Implemented
- Generate a new Bitcoin mainnet legacy address and WIF private key locally using bitcoinj.
- Check a public Bitcoin address balance and transaction count via mempool.space.
- Build an APK using GitHub Actions.

## Build on a phone
1. Create a repository at https://github.com/new named `LostBTC`.
2. Upload every file in this archive and preserve folder structure (including hidden `.github`).
3. Open **Actions** and enable workflows if asked.
4. Select **Build Android APK** → **Run workflow**.
5. Download the `LostBTC-debug-apk` artifact ZIP and extract `app-debug.apk`.

## Security / current limits
- The private key is displayed on screen and is not uploaded by the app. Do not screenshot it or commit it to GitHub; back it up offline.
- This starter does not automatically sweep funds from discovered addresses and does not yet sign or broadcast transactions.
- A public address balance does not reveal the private key or prove that a wallet is abandoned.
