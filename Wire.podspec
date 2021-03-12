Pod::Spec.new do |s|
  s.name          = 'Wire'
  s.version       = "#{ENV['POD_VERSION']}"
  s.license       = { :type => 'apache2', :file => 'LICENSE.txt' }
  s.homepage      = 'https://github.com/square/wire'
  s.authors       = { 'Eric Firestone' => '@firetweet' }
  s.summary       = 'Protocol buffers runtime for Swift.'
  s.source        = { :git => 'https://github.com/square/wire.git', :tag => "#{ENV['POD_VERSION']}" }
  s.module_name   = 'Wire'
  s.swift_version = '5.0'

  s.ios.deployment_target  = '10.0'
  s.osx.deployment_target  = '10.15'

  s.source_files  = 'wire-library/wire-runtime-swift/src/main/swift/*.swift'

  s.test_spec do |test_spec|
    test_spec.ios.deployment_target  = '11.0'
    test_spec.osx.deployment_target  = '10.15'

    test_spec.script_phase = {
      :name => 'Compile Test Protos',
      :execution_position => :before_compile,
      # We regenerate the protos on each build in case changes were made to the .proto files.
      # If a new file was added then `pod install` or `pod gen` may need to be run again.
      :script => <<-CMD
        cd ${PODS_ROOT}/../..
        if [ ! -f ./gradlew ]; then
          # This is not development mode.
          # This pod was installed via a Podfile (not from source) and ./gradlew was removed.
          exit 0
        fi
        ./gradlew -p wire-library :wire-runtime-swift:generateTestProtos
      CMD
    }

    test_spec.preserve_paths = 'wire-library/wire-runtime-swift/src/test/proto/**/*.proto'
    test_spec.source_files = 'wire-library/wire-runtime-swift/src/test/swift/**/*.swift'
  end

end
