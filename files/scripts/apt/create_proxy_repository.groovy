import groovy.json.JsonSlurper
import org.sonatype.nexus.repository.config.Configuration
import org.sonatype.nexus.repository.manager.RepositoryManager

RepositoryManager repositoryManager = container.lookup(RepositoryManager.class.getName())
parsed_args = new JsonSlurper().parseText(args)

Configuration configuration = repositoryManager.newConfiguration()
configuration.with{
    repositoryName = parsed_args.name
    recipeName = 'apt-proxy'
    online = true
    attributes = [
        storage: [
            blobStoreName: parsed_args.blob_store,
            strictContentTypeValidation: Boolean.valueOf(parsed_args.strict_content_validation)
        ],
        cleanup: [
            policyName: new HashSet<String>([parsed_args.clean_policy]) 
        ],
        proxy: [
            remoteUrl: parsed_args.remote_url,
            contentMaxAge: parsed_args.content_max_age,
            metadataMaxAge: parsed_args.metadata_max_age,
        ],
        negativeCache: [
            enabled: Boolean.valueOf(parsed_args.negative_cache_enabled),
            timeToLive: parsed_args.negative_cache_time_to_live,
        ],
        httpClient: [
            blocked: false,
            autoBlock: true,
        ],
        apt: [
            distribution: parsed_args.distribution,
            flat: Boolean.valueOf(parsed_args.flat_repo),
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
