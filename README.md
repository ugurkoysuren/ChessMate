# Blind Chess Discord Bot

Play chess against Stockfish through Discord. The bot evaluates your moves and tells you if they're good or bad.

## Setup

### 1. Install Stockfish

```bash
# macOS
brew install stockfish

# Ubuntu
sudo apt-get install stockfish
```

### 2. Create Discord Bot

1. Go to https://discord.com/developers/applications
2. Create new application
3. Go to Bot section, click Add Bot
4. Enable "Message Content Intent"
5. Copy the bot token
6. Go to OAuth2 > URL Generator
   - Scopes: `bot`
   - Permissions: `Send Messages`, `Read Message History`
7. Use the URL to invite bot to your server

### 3. Run

```bash
export DISCORD_BOT_TOKEN=your_token
export OPENROUTER_API_KEY=your_key  # optional, for AI comments

java -jar target/blind-chess-1.0.0.jar
```

Or with Maven:
```bash
mvn spring-boot:run
```

### 4. Build

```bash
mvn clean package -DskipTests
```

## Discord Commands

```
!chess new [white|black]  - Start new game
!chess move e2e4          - Make a move (UCI notation)
!chess show               - Show board
!chess legal              - List legal moves
!chess resign             - Give up
!chess help               - Show commands
```

## Move Notation

Use UCI format: source square + destination square

- `e2e4` - pawn from e2 to e4
- `g1f3` - knight from g1 to f3
- `e7e8q` - pawn promotion to queen

## Web Interface

Open http://localhost:8080 to watch games live.

## Deploy to Railway

```bash
railway login
railway init
railway variables set DISCORD_BOT_TOKEN=your_token
railway variables set OPENROUTER_API_KEY=your_key
railway up
```

## Environment Variables

| Variable | Required | Description |
|----------|----------|-------------|
| DISCORD_BOT_TOKEN | Yes | Discord bot token |
| OPENROUTER_API_KEY | No | For AI move comments |
| STOCKFISH_PATH | No | Custom stockfish path |
| PORT | No | Server port (default: 8080) |
