# PolarAlerts

A simple **SPIGOT** plugin that sends Polar anticheat alerts across your BungeeCord network via plugin messaging.

## What it does

- Forwards alerts from Polar to all your servers
- Staff can see alerts from any server on your network
- Works with regular BungeeCord - no extra plugins needed
- Supports all Polar alert types
- Secures communications with automatic secret key validation

## Setup

### Network Setup (Two Options)

#### Option 1: Alert Sending + Receiving
Put `PolarAlerts.jar` on:
- Servers with Polar installed (to send alerts)
- Any server where staff should see alerts

#### Option 2: Listen-Only Mode
For servers without Polar that just need to display alerts:
1. Put `PolarAlerts.jar` in the plugins folder
2. Edit config.yml and set `listen-only-mode: true`

This mode doesn't require Polar to be installed on the server with this plugin.

## Security

PolarAlerts secures messaging between servers using a secret key:

- A random key is generated on first plugin start
- The key is saved in your config.yml under `security.secret-key`
- Messages with wrong keys are rejected
- **Important**: All servers in your network must have the same key

### Setting Up On Multiple Servers

When adding a new server:

1. **Copy Method (easiest)**: Copy the config.yml from an existing server
2. **Manual Setup**: Edit the new server's config and set the same key as your other servers

For manual setup:
1. Find the key in your first server's config.yml
2. Add it to all other servers' configs 
3. Restart the servers

### Security Tips

- Don't share your key with anyone
- If your key might be compromised, change it on all servers
- For extra security, make sure config files are only readable by admin users

## Configuration

### Basic config (works for most servers)

```yaml
# Listen-only mode for servers without Polar
listen-only-mode: false  # Set to true on servers without Polar

# Server name that shows in alerts
server-name: 'survival'

# Security settings
security:
  # Secret key for validating messages
  # Will be auto-generated on first start
  # Copy this key to all your servers

# Display settings
display:
  enabled: true
  permission: "anticheat.alerts"
```

### Full config options

```yaml
# Listen-only mode (set to true for servers without Polar)
# When true, the plugin will only receive and display alerts, not send them
listen-only-mode: false

# Debug mode (enables extra logging)
debug: false

# Your server name (if left blank, will try to use Bukkit server ID)
# Only used when not in listen-only mode
server-name: ''

# Security configuration
security:
  # Secret key will be automatically generated on first boot if not present
  # DO NOT SHARE THIS KEY - it is used to validate message authenticity
  # All Servers that you want to use this plugin with MUST have the same secret-key value
  # secret-key: "will-be-auto-generated"

# Control which alert types are processed
# Only used when not in listen-only mode
alerts:
  detection: true
  cloud: true
  mitigation: true
  # How often to send mitigation alerts (1 = every alert, 5 = every 5th alert, etc.)
  mitigation-frequency: 1
  punishment: true

# Forwarding configuration
# Only used when not in listen-only mode
forward:
  # Standard BungeeCord forwarding (works with vanilla BungeeCord)
  bungee:
    enabled: true
    # Send to all servers? Set to false if using targeted forwarding below
    forward-to-all: true
    # Target specific servers for each alert type
    # Only used if forward-to-all is false
    targets:
      DETECTION:
        - 'lobby'
        - 'staff'
      CLOUD:
        - 'lobby'
        - 'staff'
      MITIGATION:
        - 'staff'
      PUNISHMENT:
        - 'ALL'  # Special value that forwards to all servers

  # Custom forwarding (for custom BungeeCord plugins)
  custom:
    enabled: false

# Display settings for receiving alerts
# Used in both regular and listen-only modes
display:
  # Enable displaying alerts to staff
  enabled: true
  
  # Permission required to see alerts
  permission: "anticheat.alerts"
  
  # Which alert types to display
  alert-types:
    - "DETECTION"
    - "CLOUD"
    - "MITIGATION"
    - "PUNISHMENT"
  
  # Whether to log alerts to console
  console-log: true
  
  # Make alerts clickable (clicking will run /server command)
  clickable: true
  
  # Show detailed information when hovering over alerts
  show-details-in-hover: true
  
  # Text shown when hovering over an alert
  hover-text: "§7Click to connect to §f%server%\n§7Player: §f%player%\n§7Check: §f%check%\n§7Type: §f%type%\n§7VL: §f%vl%"
  
  # Alert formats for different types
  # Available placeholders:
  # %server% - Server name
  # %player% - Player name
  # %check% - Check name
  # %type% - Check type/category
  # %vl% - Violation level
  formats:
    # Detection alert format
    detection: "§7[§b❀§7] §7[%server%] §f%player% failed §b%check% §fVL: %vl%"
    # Cloud detection alert format
    cloud: "§7[§b☁§7] §7[%server%] §f%player% failed §b%type% §7(%check%)"
    # Mitigation alert format
    mitigation: "§7[§b❀§7] §7[%server%] §f%player% mitigated §b%check% §fVL: %vl%"
    # Punishment alert format
    punishment: "§7[§c⚠§7] §7[%server%] §f%player% was punished for §b%check%"
```

## Permissions

- `anticheat.alerts` - Lets staff see alerts

## Setup Examples

### Example 1: Survival network with lobby

On survival server (has Polar installed):
```yaml
listen-only-mode: false
server-name: 'survival'
# security.secret-key will be auto-generated
```

On lobby server (doesn't have Polar):
```yaml
listen-only-mode: true
display:
  enabled: true
# IMPORTANT: Copy the security.secret-key from your survival server
```

### Example 2: Staff-only alerts server

On all gameplay servers (with Polar):
```yaml
listen-only-mode: false
server-name: 'server1'  # Unique name for each server
forward:
  bungee:
    enabled: true
    forward-to-all: false
    targets:
      DETECTION:
        - 'staff'  # Only send to staff server
# Make sure all servers share the same security.secret-key
```

On staff server (without Polar):
```yaml
listen-only-mode: true
display:
  enabled: true
# IMPORTANT: Copy the security.secret-key from your gameplay servers
```

## Troubleshooting

- **No alerts showing up?** Make sure Polar is working on at least one server
- **Listen-only mode not working?** Check that at least one server is sending alerts
- **Staff can't see alerts?** Make sure they have the `anticheat.alerts` permission
- **Not receiving alerts from other servers?** Make sure all servers have the same secret key

## Support

Need help?
- Discord: [Link](https://polar.top/discord)
