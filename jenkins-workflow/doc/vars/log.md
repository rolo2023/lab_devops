# log

Formats and logs a message in the build log

## Default parameters

* message - The string to print, very helpful way to make substitutions work

## Optional parameters

* level - Any of 'WARNING', 'ERROR', 'DEBUG' or 'INFO'. Defaults to 'INFO'

## Example

```yml
  - use: log
    with_params:
      message: "Hello, {{ repo.author_name }}"
```
