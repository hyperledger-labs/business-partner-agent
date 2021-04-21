{{/*
Expand the name of the chart.
*/}}
{{- define "global.name" -}}
{{- default .Chart.Name .Values.global.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "global.fullname" -}}
{{- if .Values.global.fullnameOverride }}
{{- .Values.global.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.global.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}


{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "global.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified bpa name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
*/}}
{{- define "bpa.fullname" -}}
{{ template "global.fullname" . }}-{{ .Values.bpa.name }}
{{- end -}}

{{/*
Create a default fully qualified acapy name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
*/}}
{{- define "acapy.fullname" -}}
{{ template "global.fullname" . }}-{{ .Values.acapy.name }}
{{- end -}}

{{/*
Common bpa labels
*/}}
{{- define "bpa.labels" -}}
helm.sh/chart: {{ include "global.chart" . }}
{{ include "bpa.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector bpa labels
*/}}
{{- define "bpa.selectorLabels" -}}
app.kubernetes.io/name: {{ include "global.fullname" . }}-{{ .Values.bpa.name }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "bpa.serviceAccountName" -}}
{{- if .Values.bpa.serviceAccount.create }}
{{- default (include "bpa.fullname" .) .Values.bpa.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.bpa.serviceAccount.name }}
{{- end }}
{{- end }}

{{/*
generate hosts if not overriden
*/}}
{{- define "bpa.host" -}}
{{- if .Values.bpa.ingress.hosts -}}
{{- (index .Values.bpa.ingress.hosts 0).host -}}
{{- else }}
{{- include "global.fullname" . }}{{ .Values.global.ingressSuffix -}}
{{- end -}}
{{- end }}

{{/*
Common acapy labels
*/}}
{{- define "acapy.labels" -}}
helm.sh/chart: {{ include "global.chart" . }}
{{ include "acapy.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector acapy labels
*/}}
{{- define "acapy.selectorLabels" -}}
app.kubernetes.io/name: {{ include "global.fullname" . }}-{{ .Values.acapy.name }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}


{{/*
generate hosts if not overriden
*/}}
{{- define "acapy.host" -}}
{{- if .Values.acapy.ingress.hosts -}}
{{- (index .Values.acapy.ingress.hosts 0).host -}}
{{- else }}
{{- include "acapy.fullname" . }}{{ .Values.global.ingressSuffix -}}
{{- end -}}
{{- end }}

{{/*
Create a default fully qualified app name for the postgres requirement.
*/}}
{{- define "global.postgresql.fullname" -}}
{{- $postgresContext := dict "Values" .Values.postgresql "Release" .Release "Chart" (dict "Name" "postgresql") -}}
{{ template "postgresql.primary.fullname" $postgresContext }}
{{- end -}}

{{/*
Create the name for the database secret.
*/}}
{{- define "global.externalDbSecret" -}}
{{- if .Values.global.persistence.existingSecret -}}
  {{- .Values.global.persistence.existingSecret -}}
{{- else -}}
  {{- template "global.fullname" . -}}-db
{{- end -}}
{{- end -}}

{{/*
Create the name for the password secret key.
*/}}
{{- define "global.dbPasswordKey" -}}
{{- if .Values.global.persistence.existingSecret -}}
  {{- .Values.global.persistence.existingSecretKey -}}
{{- else -}}
  password
{{- end -}}
{{- end -}}

{{/*
Create environment variables for database configuration.
*/}}
{{- define "global.externalDbConfig" -}}
- name: DB_VENDOR
  value: {{ .Values.global.persistence.dbVendor | quote }}
{{- if eq .Values.global.persistence.dbVendor "POSTGRES" }}
- name: POSTGRES_PORT_5432_TCP_ADDR
  value: {{ .Values.global.persistence.dbHost | quote }}
- name: POSTGRES_PORT_5432_TCP_PORT
  value: {{ .Values.global.persistence.dbPort | quote }}
- name: POSTGRES_USER
  value: {{ .Values.global.persistence.dbUser | quote }}
- name: POSTGRES_PASSWORD
  valueFrom:
    secretKeyRef:
      name: {{ template "global.externalDbSecret" . }}
      key: {{ include "global.dbPasswordKey" . | quote }}
- name: POSTGRES_DATABASE
  value: {{ .Values.global.persistence.dbName | quote }}
{{- else if eq .Values.global.persistence.dbVendor "MYSQL" }}
- name: MYSQL_PORT_3306_TCP_ADDR
  value: {{ .Values.global.persistence.dbHost | quote }}
- name: MYSQL_PORT_3306_TCP_PORT
  value: {{ .Values.global.persistence.dbPort | quote }}
- name: MYSQL_USER
  value: {{ .Values.global.persistence.dbUser | quote }}
- name: MYSQL_PASSWORD
  valueFrom:
    secretKeyRef:
      name: {{ template "global.externalDbSecret" . }}
      key: {{ include "global.dbPasswordKey" . | quote }}
- name: MYSQL_DATABASE
  value: {{ .Values.global.persistence.dbName | quote }}
{{- end }}
{{- end -}}