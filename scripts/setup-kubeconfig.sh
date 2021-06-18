
#!/bin/bash
# #### Instructions to fill the KUBE_CONFIG_FILE env var
#  4. Go to https://gitpod.io/settings/ and create:
#     - name: KUBE_CONFIG_FILE
#     - value: paste-the-output-of-your-kubeconfig-file (make sure it is jason not yaml)
#     - scope: */*

KUBE_CONFIG_PATH="/home/gitpod/.kube/config"
if [ ! -f "$KUBE_CONFIG_PATH" ]; then
    if [ -z "$KUBE_CONFIG_FILE" ]; then
        echo "KUBE_CONFIG_FILE not set, doing nothing."
        return;
    fi
    echo "$KUBE_CONFIG_FILE" > "$KUBE_CONFIG_PATH"
    echo "Add k8s config based on contents from KUBE_CONFIG_FILE"
fi