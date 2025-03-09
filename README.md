# PolarAlerts

A simple plugin that sends Polar anticheat alerts across your BungeeCord network.

## What it does

- Forwards alerts from Polar to all your servers
- Staff can see alerts from any server on your network
- Works with regular BungeeCord - no extra plugins needed
- Supports all Polar alert types

## Setup

1. Put `PolarAlerts.jar` in the plugins folder on:
   - Servers running Polar (to send alerts)
   - Servers where staff should see alerts
2. Restart your servers
3. Edit the config file if needed

## Configuration

### Basic config (works for most servers)

```yaml
# Server name that shows in alerts
server-name: 'survival'

# Display settings
display:
  enabled: true
  permission: "anticheat.alerts"
```

### Full config options

```yaml
# Server name in alerts
server-name: 'survival'

# Which alert types to forward
alerts:
  detection: true
  cloud: true
  mitigation: true
  punishment: true

# Forwarding settings
forward:
  bungee:
    enabled: true
    forward-to-all: true

# Display settings
display:
  enabled: true
  permission: "anticheat.alerts"
  
  # Which alert types to show
  alert-types:
    - "DETECTION"
    - "CLOUD"
    - "MITIGATION"
    - "PUNISHMENT"
  
  # Change how alerts look
  formats:
    detection: "§c[%server%] §f%player% §7failed §f%check% §7check (%type%) §8[§f%vl%§8]"
    cloud: "§c[%server%] §f%player% §7failed §f%type% §7cloud check"
```

## Permissions

- `anticheat.alerts` - Lets staff see alerts

## Troubleshooting

- **No alerts showing up?** Make sure Polar is working and both servers have the plugin
- **Plugin not loading?** Check that you have Polar installed
- **Staff can't see alerts?** Make sure they have the `anticheat.alerts` permission

## Support

Need help?
- Discord: [Link](https://polar.top/discord)
