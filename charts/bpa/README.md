# bpa

The Business Partner Agent allows to manage and exchange master data between organizations.

![Version: 0.1.0-alpha2](https://img.shields.io/badge/Version-0.1.0--alpha2-informational?style=flat-square) ![Type: application](https://img.shields.io/badge/Type-application-informational?style=flat-square) ![AppVersion: latest](https://img.shields.io/badge/AppVersion-latest-informational?style=flat-square)

This chart will install BPA (bpa-core & bpa-acapy) and Postgres.

It will also create the default ingress routes.

## TL;DR

```sh
helm repo add bpa https://hyperledger-labs.github.io/business-partner-agent/
helm repo update
helm install \
	--set bpa.image.repository=myrepo.io/bpa \
	--set bpa.image.tag=latest \
	--set bpa.acapy.agentSeed=12345678901234567890123456789012 \
   	mybpa bpa/bpa -n mynamespace --devel
```

## Introduction

This chart bootstraps a business partner agent deployment on a Kubernetes cluster using the Helm package manager. Its default installation comes with PostgreSQL. Ingress routes can be activated, allowing the agent to
communicate with other agents outside the cluster.

## Requirements

- Kubernetes 1.12+
- Docker
- Helm v3.3.4+
- PV provisioner support in the underlying infrastructure (for PostgreSQL persistence)
- If activating Ingress routes:
  - Ingress controller installed
  - Cert-manager
  - DNS records pointing to your routes 

## Initial preparation

The following steps have to be done only once.

### Clone this git repository

This  is required for the next preparation steps.

```s
git@github.com:hyperledger-labs/business-partner-agent.git
cd business-partner-agent
```

### Create and push docker image

In the future we plan to have bpa image publically available, e.g. on docker hub.
Currently you have to build it on your own and make it available in a registry (one that is reachable by your kubernetes cluster, e.g. docker hub).

Build your image by executing the docker build command and push it to you registry.

```s
docker login --username=yourusername --password=yourpassword
docker build -t myrepo.io/bpa:latest .
sudo docker push myrepo.io/bpa:latest
```

Use the `./scripts/register-did.sh` script to register a new DID on our test network (see also [main documentation](../../README.md)
Just run:

```s
./scripts/register-did.sh
```

## Install the chart

To install the chart with the release name `bpa`, the docker image `myrepo.io/bpa` and the seed `12345678901234567890123456789012` in the namespace `mynamespace`

```sh
helm repo add bpa https://hyperledger-labs.github.io/business-partner-agent/
helm repo update
helm install \
	--set bpa.image.repository=myrepo.io/bpa \
	--set bpa.image.tag=latest \
	--set bpa.acapy.agentSeed=12345678901234567890123456789012 \
   	mybpa bpa/bpa -n mynamespace --devel
```

The command deploys BPA (bpa-core & bpa-acapy) and Postgres on the Kubernetes cluster in the default configuration. The [Parameters](#Parameters) sections list the parameter that can be configured during installation.

## Uninstalling the Chart

To uninstall/delete the my-release deployment:

```sh
helm delete mybpa
```

The command removes all the Kubernetes components but PVC's associated with the chart and deletes the release.

To delete the PVC's associated with my-release:

```sh
kubectl delete pvc -l release=mybpa
```

Note: Deleting the PVC's will delete postgresql data as well. Please be cautious before doing it.
culpa qui officia deserunt mollit anim id est laborum.

## Installing the Chart

To install the chart with the release name `my-release`:

```console
$ helm repo add foo-bar http://charts.foo-bar.com
$ helm install my-release foo-bar/bpa
```

## Values

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| acapy.adminURLApiKey | string | `"2f9729eef0be49608c1cffd49ee3cc4a"` |  |
| acapy.affinity | object | `{}` |  |
| acapy.agentName | string | `"ca-aca-py"` |  |
| acapy.agentSeed | string | `nil` |  |
| acapy.fullnameOverride | string | `""` |  |
| acapy.image.pullPolicy | string | `"IfNotPresent"` |  |
| acapy.image.repository | string | `"bcgovimages/aries-cloudagent"` |  |
| acapy.image.tag | string | `"py36-1.15-0_0.5.6"` |  |
| acapy.imagePullSecrets | list | `[]` |  |
| acapy.ingress.annotations | object | `{}` |  |
| acapy.ingress.enabled | bool | `false` |  |
| acapy.ingress.hosts[0].host | string | `"my-acapy.local"` |  |
| acapy.ingress.hosts[0].paths | list | `[]` |  |
| acapy.ingress.tls | list | `[]` |  |
| acapy.name | string | `"acapy"` |  |
| acapy.nameOverride | string | `""` |  |
| acapy.nodeSelector | object | `{}` |  |
| acapy.podAnnotations | object | `{}` |  |
| acapy.podSecurityContext | object | `{}` |  |
| acapy.resources | object | `{}` |  |
| acapy.securityContext | object | `{}` |  |
| acapy.service.adminPort | int | `8031` |  |
| acapy.service.httpPort | int | `8030` |  |
| acapy.service.type | string | `"ClusterIP"` |  |
| acapy.tolerations | list | `[]` |  |
| bpa.affinity | object | `{}` |  |
| bpa.didPrefix | string | `"did:sov:iil:"` |  |
| bpa.image.pullPolicy | string | `"IfNotPresent"` |  |
| bpa.image.repository | string | `"myrepo"` |  |
| bpa.image.tag | string | `""` |  |
| bpa.imagePullSecrets | list | `[]` |  |
| bpa.ingress.annotations | object | `{}` |  |
| bpa.ingress.enabled | bool | `false` |  |
| bpa.ingress.hosts[0].host | string | `"my-bpa.local"` |  |
| bpa.ingress.hosts[0].paths | list | `[]` |  |
| bpa.ingress.tls | list | `[]` |  |
| bpa.ledgerBrowser | string | `"https://indy-test.bosch-digital.de"` |  |
| bpa.ledgerURL | string | `"https://indy-test.bosch-digital.de"` |  |
| bpa.name | string | `"bpacore"` |  |
| bpa.nodeSelector | object | `{}` |  |
| bpa.password | string | `"changeme"` |  |
| bpa.podAnnotations | object | `{}` |  |
| bpa.podSecurityContext | object | `{}` |  |
| bpa.resolverURL | string | `"https://resolver.dev.economyofthings.io"` |  |
| bpa.resources | object | `{}` |  |
| bpa.securityContext | object | `{}` |  |
| bpa.securityEnabled | bool | `true` |  |
| bpa.service.port | int | `80` |  |
| bpa.service.type | string | `"ClusterIP"` |  |
| bpa.tolerations | list | `[]` |  |
| bpa.userName | string | `"admin"` |  |
| bpa.webMode | bool | `false` |  |
| global.fullnameOverride | string | `""` |  |
| global.nameOverride | string | `""` |  |
| global.persistence.deployPostgres | bool | `true` |  |
| postgresql.persistence.enabled | bool | `false` |  |
| postgresql.postgresPassword | string | `"bpa"` |  |
| postgresql.postgresqlDatabase | string | `"bpa"` |  |
| postgresql.postgresqlUsername | string | `"bpa"` |  |
| postgresql.service.port | int | `5432` |  |

----------------------------------------------
Autogenerated from chart metadata using [helm-docs v1.4.0](https://github.com/norwoodj/helm-docs/releases/v1.4.0)