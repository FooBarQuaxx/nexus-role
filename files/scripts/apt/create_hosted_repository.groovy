import groovy.json.JsonSlurper
import org.sonatype.nexus.repository.config.Configuration
import org.sonatype.nexus.repository.manager.RepositoryManager

repositoryManager = container.lookup(RepositoryManager.class.getName())
parsed_args = new JsonSlurper().parseText(args)

Configuration configuration = repositoryManager.newConfiguration()
configuration.with{
    repositoryName = parsed_args.name
    recipeName = 'apt-hosted'
    online = true
    attributes = [
        storage: [
            blobStoreName: parsed_args.blob_store,
            strictContentTypeValidation: Boolean.valueOf(parsed_args.strict_content_validation),
            writePolicy: parsed_args.write_policy.toUpperCase(),
        ],
        cleanup: [
            policyName: new HashSet<String>([parsed_args.clean_policy]) 
        ],
        apt: [
            distribution: parsed_args.distribution,
        ],
        aptSigning: parsed_args.apt_signing,
    ]
}

def existingRepository = repositoryManager.get(parsed_args.name)

if (existingRepository != null) {
    existingRepository.stop()
    configuration.attributes['storage']['blobStoreName'] = existingRepository.configuration.attributes['storage']['blobStoreName']
    existingRepository.update(configuration)
    existingRepository.start()
} else {
    repositoryManager.create(configuration)
}
