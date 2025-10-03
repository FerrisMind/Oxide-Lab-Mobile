# Kotlin Linting Setup

This project uses two linters for Kotlin code:
1. [ktlint](https://ktlint.github.io/) - An anti-bikeshedding Kotlin linter with built-in formatter
2. [detekt](https://detekt.dev/) - A static code analysis tool for Kotlin

## Configuration

### ktlint
- Configuration: [.editorconfig](../.editorconfig)
- Rules: ktlint uses standard Kotlin style guide with some customizations in .editorconfig

### detekt
- Configuration: [config/detekt/detekt.yml](../config/detekt/detekt.yml)
- Rules: Custom ruleset based on detekt's default configuration

## Running Linters

### Using Gradle Tasks

```bash
# Run ktlint check
./gradlew ktlintCheck

# Run ktlint format (auto-fixes issues)
./gradlew ktlintFormat

# Run detekt
./gradlew detekt

# Run both linters
./gradlew ktlintCheck detekt
```

### Using Scripts

```bash
# Unix/Linux/macOS
./scripts/lint.sh

# Windows
scripts\lint.bat
```

## Continuous Integration

The linters are configured to run as part of the build process. The build will fail if any linting issues are found.

## IDE Integration

### Android Studio/IntelliJ IDEA

1. Install the ktlint plugin
2. Install the detekt plugin
3. Configure the plugins to use the project's configuration files

### VS Code

1. Install the Kotlin extension
2. Install the ktlint extension
3. Configure the extensions to use the project's configuration files

## Customizing Rules

### ktlint

Modify the [.editorconfig](../.editorconfig) file to change ktlint rules.

### detekt

Modify the [config/detekt/detekt.yml](../config/detekt/detekt.yml) file to change detekt rules.

## Auto-formatting

ktlint can automatically format your code to match the style guide:

```bash
# Format all Kotlin files
./gradlew ktlintFormat
```

## Troubleshooting

### Common Issues

1. **"Task not found" errors**: Make sure you're running the commands from the project root directory
2. **Configuration not picked up**: Ensure the configuration files are in the correct locations
3. **IDE integration not working**: Check that the IDE plugins are installed and configured correctly

### Updating Configurations

To update the detekt configuration with the latest default rules:

```bash
./gradlew detektGenerateConfig
```

This will generate a new detekt.yml file with the default configuration that you can merge with the existing one.