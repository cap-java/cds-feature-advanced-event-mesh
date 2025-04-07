[![REUSE status](https://api.reuse.software/badge/github.com/cap-java/cds-feature-advanced-event-mesh)](https://api.reuse.software/info/github.com/cap-java/cds-feature-advanced-event-mesh)

# cds-feature-advanced-event-mesh

## About this project

CDS plugin providing integration with SAP Integration Suite, advanced event mesh.

## Requirements and Setup

See [Getting Started](https://cap.cloud.sap/docs/get-started/in-a-nutshell?impl-variant=java) on how to jumpstart your development and grow as you go with SAP Cloud Application Programming Model.

### SAP Integration Suite, Advanced Event Mesh

For details on how to use SAP Integration Suite, advanced event mesh, please see the [SAP Integration Suite, Advanced Event Mesh Service Guide](https://help.sap.com/docs/sap-integration-suite/advanced-event-mesh/cap-plugin-for-sap-integration-suite-advanced-event-mesh).

### CDS Plugin

The usage of CAP Java plugins is described in the [CAP Java Documentation](https://cap.cloud.sap/docs/java/building-plugins#reference-the-new-cds-model-in-an-existing-cap-java-project). Following this documentation this plugin needs to be referenced in the `srv/pom.xml` of a CAP Java project:

```xml
<dependency>
    <groupId>com.sap.cds</groupId>
    <artifactId>cds-feature-advanced-event-mesh</artifactId>
    <version>${latest-version}</version>
</dependency>
```

The latest version can be found in the [changelog](./CHANGELOG.md) or in the [Maven Central Repository](https://central.sonatype.com/artifact/com.sap.cds/cds-feature-advanced-event-mesh/versions).

## Support, Feedback, Contributing

This project is open to feature requests/suggestions, bug reports etc. via [GitHub issues](https://github.com/cap-java/cds-feature-advanced-event-mesh/issues). Contribution and feedback are encouraged and always welcome. For more information about how to contribute, the project structure, as well as additional contribution information, see our [Contribution Guidelines](CONTRIBUTING.md).

## Security / Disclosure

If you find any bug that may be a security problem, please follow our instructions at [in our security policy](https://github.com/cap-java/cds-feature-advanced-event-mesh/security/policy) on how to report it. Please do not create GitHub issues for security-related doubts or problems.

## Code of Conduct

We as members, contributors, and leaders pledge to make participation in our community a harassment-free experience for everyone. By participating in this project, you agree to abide by its [Code of Conduct](https://github.com/cap-java/.github/blob/main/CODE_OF_CONDUCT.md) at all times.

## Licensing

Copyright 2025 SAP SE or an SAP affiliate company and cds-feature-advanced-event-mesh contributors. Please see our [LICENSE](LICENSE) for copyright and license information. Detailed information including third-party components and their licensing/copyright information is available [via the REUSE tool](https://api.reuse.software/info/github.com/cap-java/cds-feature-advanced-event-mesh).
