# bpa

The Business Partner Agent allows to manage and exchange master data between organizations.

![Version: 0.1.0-alpha2](https://img.shields.io/badge/Version-0.1.0--alpha2-informational?style=flat-square) ![Type: application](https://img.shields.io/badge/Type-application-informational?style=flat-square) ![AppVersion: latest](https://img.shields.io/badge/AppVersion-latest-informational?style=flat-square)

This chart will install a business partner agent (bpa-core & bpa-acapy) and Postgres.

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

This chart bootstraps a business partner agent deployment on a Kubernetes cluster using the Helm package manager. Its default installation comes with PostgreSQL. Ingress can be activated, allowing the agent to communicate with other agents outside the cluster.

## Requirements

- Kubernetes 1.12+
- Docker
- Helm v3.3.4+
- PV provisioner support in the underlying infrastructure (for PostgreSQL persistence)
- If activating Ingress:
  - Ingress controller installed
  - Cert-manager
  - DNS records pointing to your routes 

## Initial preparation

The following steps have to be done only once.

### Clone this git repository

This  is required for the next preparation steps.

```s
git clone git@github.com:hyperledger-labs/business-partner-agent.git
cd business-partner-agent/docker
```

### Create and push docker image

In the future we plan to have bpa image publically available, e.g. on docker hub.
Currently you have to build it on your own and make it available in a registry (one that is reachable by your kubernetes cluster, e.g. docker hub).

Build your image by executing the docker build command and push it to your registry.
E.g.

```s
docker login --username=yourusername --password=yourpassword
docker build -t myrepo.io/bpa:latest .
docker push myrepo.io/bpa:latest
```
See also [docker command line documentation](https://docs.docker.com/engine/reference/commandline/cli/).

### Register a new DID

Use the `./docker/register-did.sh` script to register a new DID on our test network (see also [docker setup](../../docker/README.md))
Just run:

```s
./register-did.sh
```

## Installing the chart

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

Get the application URL by running the commands returned by helm install, e.g.:
```sh
export POD_NAME=$(kubectl get pods --namespace md -l "app.kubernetes.io/name=bpa-bpacore,app.kubernetes.io/instance=mynamespace" -o jsonpath="{.items[0].metadata.name}")
echo "Visit http://127.0.0.1:8080 to use your application"
kubectl --namespace mynamespace port-forward $POD_NAME 8080:8080
```

This deploys BPA (bpa-core & bpa-acapy) and Postgres on the Kubernetes cluster in the default configuration. The [Values](#Values) section list the parameters that can be configured during installation.

#### Install chart with values file

Alternatively, a YAML file that specifies the values for the parameters can be provided while installing the chart.
Deploying the charts with configured ingress routes could be done e.g. as follows.

Create a yaml file.
```yaml
cat <<EOT >> values-mybpa.yaml
bpa:
   image:
     repository: myrepo.io/bpa
     tag: latest
   ingress:
     enabled: true
     hosts:
     - host: mybpa.example.com
       paths:
       - /
acapy:
   agentSeed: 12345678901234567890123456789012
   ingress:
     enabled: true
     hosts:
     - host: mybpa-acapy.example.com
       paths:
       - /
EOT
```

Install the chart with the release name `mybpa`, in the namespace `mynamespace`.

```sh
helm install \
	--values values-mybpa.yaml \
   	mybpa bpa/bpa -n mynamespace --devel 
```
#### Installl multiple bpa instances

> You could easily deploy a second business partner agent like this, e.g. for demo purpose.
> Just use a different helm release name, the seed of another DID and different ingress host names.

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

## Values

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| acapy.adminURLApiKey | string | `"2f9729eef0be49608c1cffd49ee3cc4a"` |  |
| acapy.affinity | object | `{}` |  |
| acapy.agentName | string | `"ca-aca-py"` |  |
| acapy.agentSeed | String | `nil` | The agent seed, 32 characters. See main documentation.  |
| acapy.fullnameOverride | string | `""` |  |
| acapy.image.pullPolicy | string | `"IfNotPresent"` |  |
| acapy.image.repository | string | `"bcgovimages/aries-cloudagent"` |  |
| acapy.image.tag | string | `"py36-1.15-0_0.5.6"` | Overrides the image tag whose default is the chart appVersion. |
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
| bpa.didPrefix | string | `"did:sov:iil:"` | The ledger prefix that is configured with the Uni Resolver |
| bpa.image.pullPolicy | string | `"IfNotPresent"` |  |
| bpa.image.repository | string | `"myrepo"` |  |
| bpa.image.tag | string | `""` | Overrides the image tag whose default is the chart appVersion. |
| bpa.imagePullSecrets | list | `[]` |  |
| bpa.ingress.annotations | object | `{}` |  |
| bpa.ingress.enabled | bool | `false` |  |
| bpa.ingress.hosts[0].host | string | `"my-bpa.local"` |  |
| bpa.ingress.hosts[0].paths | list | `[]` |  |
| bpa.ingress.tls | list | `[]` |  |
| bpa.ledgerBrowser | string | `"https://indy-test.bosch-digital.de"` | The Ledger Explorer   |
| bpa.ledgerURL | string | `"https://indy-test.bosch-digital.de"` | The Ledger URL |
| bpa.name | string | `"bpacore"` |  |
| bpa.nodeSelector | object | `{}` |  |
| bpa.password | string | `"changeme"` | Default password, overwrite default if running in production like environments |
| bpa.podAnnotations | object | `{}` |  |
| bpa.podSecurityContext | object | `{}` |  |
| bpa.resolverURL | string | `"https://resolver.dev.economyofthings.io"` | Uni Resolver URL |
| bpa.resources | object | `{}` |  |
| bpa.securityContext | object | `{}` |  |
| bpa.securityEnabled | bool | `true` | enable security (username and password) |
| bpa.service.port | int | `80` |  |
| bpa.service.type | string | `"ClusterIP"` |  |
| bpa.tolerations | list | `[]` |  |
| bpa.userName | string | `"admin"` | Default username |
| bpa.webMode | bool | `false` | Run in web only mode without any ledger dependency and aries functionality |
| global.fullnameOverride | string | `""` |  |
| global.nameOverride | string | `""` |  |
| global.persistence.deployPostgres | bool | `true` | If true, the Postgres chart is deployed |
| postgresql.persistence | object | `{"enabled":false}` | Persistent Volume Storage configuration. ref: https://kubernetes.io/docs/user-guide/persistent-volumes |
| postgresql.persistence.enabled | bool | `false` | Enable PostgreSQL persistence using Persistent Volume Claims. |
| postgresql.postgresPassword | string | `"bpa"` | PostgreSQL Password for the new user. If not set, a random 10 characters password will be used. |
| postgresql.postgresqlDatabase | string | `"bpa"` | PostgreSQL Database to create. |
| postgresql.postgresqlUsername | string | `"bpa"` | PostgreSQL User to create. |
| postgresql.service | object | `{"port":5432}` | PostgreSQL service configuration |

## Chart dependencies
| Repository | Name | Version |
|------------|------|---------|
| https://charts.bitnami.com/bitnami/ | postgresql | 9.7.2 |

## Chart development

### Publish chart(s)

See [publishing docu](../../PUBLISHING.md).

### Documentation

The chart documentation is generated via `helm-docs` out of a go template.

```sh
cd charts
docker run --rm --volume "$(pwd):/helm-docs" -u $(id -u) jnorwood/helm-docs:latest
```

## Maintainers

| Name | Email | Url |
| ---- | ------ | --- |
| Frank Bernhardt | Frank.Bernhardt@bosch.com |  |

----------------------------------------------
Autogenerated from chart metadata using [helm-docs v1.4.0](https://github.com/norwoodj/helm-docs/releases/v1.4.0)
