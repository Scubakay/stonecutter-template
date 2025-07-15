# Stonecutter Template

Based on https://github.com/stonecutter-versioning/stonecutter-template-fabric

## Setup

1. Use this template
2. Create new repository
3. Set up permissions for template sync workflow:
   - Go to repository and navigate to Settings -> Actions -> General.
   - Under "Workflow permissions," enable the Allow GitHub Actions to create and approve pull requests setting. Click Save to apply the changes.
4. Set up mod information under `mod` in gradle.properties
5. Set up versions under `settings` in gradle.properties

## Dependencies

Dependencies are automatically resolved using Fletching Table. Just add the slug to `gradle.properties`:
```properties
# Mods used in development
modrinth.runtime=fungible-updated,modmenu
# Mod dependencies
modrinth.implementation=fabric-permissions-api
# Included dependencies
modrinth.include=midnightlib
```

If you need a specific version you can add it like this:
```properties
modrinth.runtime.fungible-updated=1.0.0
```