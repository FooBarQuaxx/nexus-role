import groovy.json.JsonSlurper
import org.sonatype.nexus.repository.config.Configuration
import org.sonatype.nexus.repository.manager.RepositoryManager

RepositoryManager repositoryManager = container.lookup(RepositoryManager.class.getName())
parsed_args = new JsonSlurper().parseText(args)

Configuration configuration = repositoryManager.newConfiguration()
configuration.with{
    repositoryName = parsed_args.name
    recipeName = 'yum-proxy'
    online = true
    attributes = [
        proxy: [
            remoteUrl: parsed_args.remote_url,
            contentMaxAge: 1440,
            metadataMaxAge: 1440
        ],
        httpClient: [
            blocked: false,
            autoBlock: true,
        ],
        storage: [
            blobStoreName: parsed_args.blob_store,
            strictContentTypeValidation: Boolean.valueOf(parsed_args.strict_content_validation)
        ],
        cleanup: [
            policyName: new HashSet<String>([parsed_args.clean_policy]) 
        ]
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
