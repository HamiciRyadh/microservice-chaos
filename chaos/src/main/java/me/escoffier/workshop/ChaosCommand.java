package me.escoffier.workshop;

import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.jboss.logging.Logger;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import javax.inject.Inject;
import java.util.List;

@Command(name = "chaos", mixinStandardHelpOptions = true)
public class ChaosCommand implements Runnable {

    @CommandLine.Option(
        names = {"--deployment", "-d"}, required = true, description = "The targeted deployment")
    String deployment;

    @Inject KubernetesClient kubernetes;
    @Inject Logger logger;

    @Override
    public void run() {
        logger.infof("Connected to %s, current namespace is %s", kubernetes.getMasterUrl(), kubernetes.getNamespace());

        List<Pod> pods = kubernetes.pods().list().getItems();
        for (Pod pod : pods) {
            logger.infof("Pod %s - %s", pod.getMetadata().getName(), pod.getStatus().getPhase());
            for (OwnerReference reference : pod.getMetadata().getOwnerReferences()) {
                logger.infof("\t Owner: %s (%s)", reference.getName(), reference.getKind());
            }
        }

        List<Deployment> deployments = kubernetes.apps().deployments().list().getItems();
        for (Deployment dep : deployments) {
            logger.infof("Deployment %s", dep.getMetadata().getName());
        }
        Pod toDelete;
        while (true) {
            toDelete = null;
            for (Pod pod : kubernetes.pods().list().getItems()) {
                if (pod.getMetadata().getName().contains(deployment)) {
                    toDelete = pod;
                    break;
                }
            }
            if (toDelete != null) {
                logger.infof("Deleting Pod %s - %s ...", toDelete.getMetadata().getName(), toDelete.getStatus().getPhase());
                if (kubernetes.pods().delete(toDelete)) {
                    logger.infof("Deleted Pod %s", toDelete.getMetadata().getName());
                }
            }

            try {
                Thread.sleep(5000);
            } catch (InterruptedException ignore) {
                logger.error("Interruption exception");
            }
        }
    }
}