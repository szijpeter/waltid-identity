import WaltIcon from "@/components/walt/logo/WaltIcon";
import {CheckCircleIcon} from "@heroicons/react/24/outline";
import {useContext, useEffect, useMemo, useState} from "react";
import {useRouter} from "next/router";
import axios from "axios";
import nextConfig from "@/next.config";
import Modal from "@/components/walt/modal/BaseModal";
import {EnvContext} from "@/pages/_app";

export default function Success() {
  const env = useContext(EnvContext);
  const router = useRouter();
  const isVerifier2Engine = router.query.engine?.toString() === 'verifier2';
  const [vctName, setVctName] = useState<string | null>(null);

  const [policyResults, setPolicyResults] = useState<
    Array<{
      policyResults: Array<{
        policy: string;
        is_success: boolean;
      }>;
    }>
  >([]);
  const [credentials, setCredentials] = useState<
    Array<{
      type: Array<string>;
      vct: Array<string>;
      credentialSubject: {
        [key: string]: string;
      };
    }>
  >([]);
  const [index, setIndex] = useState<number>(0);
  const [modal, setModal] = useState<boolean>(false);
  const [verifier2SessionInfo, setVerifier2SessionInfo] = useState<any | null>(null);
  const [verifier2Error, setVerifier2Error] = useState<string | null>(null);

  function parseJwt(token: string) {
    return JSON.parse(Buffer.from(token.split('.')[1], 'base64').toString());
  }

  const fetchVctName = async (vctUrl: string) => {
    try {
      const response = await axios.get(vctUrl);
      const bodyJson = response.data;
      return bodyJson['name'];
    } catch (error) {
      console.error('Error fetching vct:', error);
      return 'Unknown VCT'; // Fallback value if the request fails
    }
  };

  useEffect(() => {
    if (!router.isReady || !router.query.sessionId) return;

    if (isVerifier2Engine) {
      const verifier2BaseUrl = env.NEXT_PUBLIC_VERIFIER2
        ? env.NEXT_PUBLIC_VERIFIER2
        : nextConfig.publicRuntimeConfig!.NEXT_PUBLIC_VERIFIER2;
      axios
        .get(
          `${verifier2BaseUrl}/verification-session/${encodeURIComponent(router.query.sessionId.toString())}/info`
        )
        .then((response) => {
          setVerifier2SessionInfo(response.data);
          setVerifier2Error(null);
        })
        .catch((error) => {
          const message = error?.response?.data?.errorDescription
            || error?.response?.data?.message
            || error?.message
            || 'Could not load verifier2 session.';
          setVerifier2Error(message);
          console.error(error);
        });
      return;
    }

    axios
      .get(
        `${env.NEXT_PUBLIC_VERIFIER ? env.NEXT_PUBLIC_VERIFIER : nextConfig.publicRuntimeConfig!.NEXT_PUBLIC_VERIFIER}/openid4vc/session/${router.query.sessionId}`
      )
      .then((response) => {
        let parsedToken = parseJwt(response.data.tokenResponse.vp_token);
        let containsVP = !!parsedToken.vp?.verifiableCredential;
        let vcs = containsVP
          ? parsedToken.vp?.verifiableCredential
          : [response.data.tokenResponse.vp_token];

        setCredentials(
          Array.isArray(vcs)
            ? vcs.map((vc: string) => {
              if (typeof vc !== 'string') {
                console.error(
                  'Invalid VC format: expected a string but got',
                  vc
                );
                return vc;
              }
              let split = vc.split('~');
              let parsed = parseJwt(split[0]);

              if (split.length === 1) return parsed.vc ? parsed.vc : parsed;
              else {
                let credentialWithSdJWTAttributes = { ...parsed };
                split.slice(1).forEach((item) => {
                  // If it is key binding jwt, skip
                  if (item.split('.').length === 3) return;

                  let parsedItem = JSON.parse(
                    Buffer.from(item, 'base64').toString()
                  );
                  credentialWithSdJWTAttributes.credentialSubject = {
                    [parsedItem[1]]: parsedItem[2],
                    ...credentialWithSdJWTAttributes.credentialSubject,
                  };
                });
                credentialWithSdJWTAttributes.type = parsed.vc?.type
                return credentialWithSdJWTAttributes;
              }
            })
            : []
        );

        setPolicyResults(() => {
          if (containsVP) {
            return response.data.policyResults.results;
          } else {
            //add a new entry to the policy results, since its start index +1
            return [
              {
                policyResults: [
                  {
                    policy: 'New Policy',
                    is_success: true,
                  },
                ],
              },
              ...response.data.policyResults.results,
            ];
          }
        });

        if (!containsVP) {
          const vct = parsedToken['vct'];
          const vctUrl = new URL(vct);
          const vctResolutionUrl = `${vctUrl.origin}/.well-known/vct${vctUrl.pathname}`;
          fetchVctName(vctResolutionUrl).then((name) => setVctName(name));
        }
      });
  }, [router.isReady, router.query.sessionId, isVerifier2Engine, env]);

  const verifier2PolicyResults = useMemo(() => {
    if (!verifier2SessionInfo) {
      return null;
    }
    return verifier2SessionInfo.policy_results
      ?? verifier2SessionInfo.policyResults
      ?? verifier2SessionInfo.authorizationRequest?.policies?.policy_results
      ?? null;
  }, [verifier2SessionInfo]);

  const verifier2PresentedCredentials = useMemo(() => {
    if (!verifier2SessionInfo) {
      return null;
    }
    return verifier2SessionInfo.presented_credentials
      ?? verifier2SessionInfo.presentedCredentials
      ?? verifier2SessionInfo.presented_presentations
      ?? verifier2SessionInfo.presented_raw_data?.vpToken
      ?? null;
  }, [verifier2SessionInfo]);

  if (isVerifier2Engine) {
    return (
      <div className="h-screen flex justify-center items-center bg-gray-50">
        <div className="relative w-full h-full sm:h-auto sm:w-10/12 md:w-8/12 lg:w-8/12 text-center shadow-2xl rounded-lg pt-8 pb-8 px-10 bg-white">
          <h1 className="text-3xl text-gray-900 text-center font-bold mb-10">
            Verifier2 Session Result
          </h1>
          <div className="text-gray-600 mb-2">
            Session ID: <span className="font-mono text-gray-800">{router.query.sessionId?.toString()}</span>
          </div>
          <div className="text-xl font-semibold text-gray-900 mb-8">
            Status: {verifier2SessionInfo?.status ?? 'Unknown'}
          </div>
          {verifier2Error && (
            <p className="text-sm text-red-600 break-all mb-6">{verifier2Error}</p>
          )}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 text-left">
            <div className="rounded-lg border border-gray-200 p-4">
              <h2 className="text-lg font-semibold text-gray-900 mb-3">Policy Results</h2>
              <pre className="text-xs text-gray-700 whitespace-pre-wrap break-all">
                {JSON.stringify(verifier2PolicyResults ?? { message: 'No policy results available.' }, null, 2)}
              </pre>
            </div>
            <div className="rounded-lg border border-gray-200 p-4">
              <h2 className="text-lg font-semibold text-gray-900 mb-3">Presented Credentials</h2>
              <pre className="text-xs text-gray-700 whitespace-pre-wrap break-all">
                {JSON.stringify(verifier2PresentedCredentials ?? { message: 'No presented credentials available.' }, null, 2)}
              </pre>
            </div>
          </div>
          <div className="flex flex-col items-center mt-12">
            <div className="flex flex-row gap-2 items-center content-center text-sm text-center text-gray-500">
              <p className="">Secured by walt.id</p>
              <WaltIcon height={15} width={15} type="gray" />
            </div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="h-screen flex justify-center items-center bg-gray-50">
      <Modal show={modal} securedByWalt={false} onClose={() => setModal(false)}>
        <div className="flex flex-col items-center">
          <div className="w-full">
            <textarea
              value={JSON.stringify(
                credentials[index]?.credentialSubject ?? credentials[index],
                null,
                4
              )}
              disabled={true}
              className="w-full h-48 border-2 border-gray-300 rounded-md px-2"
            />
          </div>
        </div>
      </Modal>
      <div className="relative w-full h-full sm:h-auto sm:w-10/12 md:w-8/12 lg:w-6/12 text-center shadow-2xl rounded-lg pt-8 pb-8 px-10 bg-white">
        <h1 className="text-3xl text-gray-900 text-center font-bold mb-10">
          Presented Credentials
        </h1>
        <div className="flex items-center justify-center">
          {index !== 0 && (
            <button
              onClick={() => setIndex(index - 1)}
              className="text-gray-500 hover:text-gray-900 focus:outline-none absolute left-10"
            >
              <svg
                xmlns="http://www.w3.org/2000/svg"
                className="h-6 w-6 mr-2"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M15 19l-7-7 7-7"
                />
              </svg>
            </button>
          )}
          <div className="group h-[225px] w-[400px] [perspective:1000px]">
            <div className="relative h-full w-full rounded-xl shadow-xl transition-all duration-500 [transform-style:preserve-3d] group-hover:[transform:rotateY(180deg)]">
              <div className="absolute inset-0">
                <div className="flex h-full w-full flex-col drop-shadow-sm rounded-xl py-7 px-8 text-gray-100 cursor-pointer overflow-hidden bg-gradient-to-r from-green-700 to-green-900 z-[-2]">
                  <div className="flex flex-row">
                    <WaltIcon height={35} width={35} outline type="white" />
                  </div>
                  <div className="mb-8 mt-12">
                    <h6 className={'text-2xl font-bold overflow-hidden text-ellipsis whitespace-nowrap'}>
                      {credentials[index]?.type
                        ? credentials[index]?.type[
                          credentials[index].type.length - 1
                        ].replace(/([a-z0-9])([A-Z])/g, '$1 $2')
                        : credentials[index]?.vct
                          ? vctName
                          : credentials[index]?.vct}
                    </h6>
                  </div>
                </div>
              </div>
              <div className="absolute inset-0 h-full w-full rounded-xl bg-white p-5 text-slate-200 [transform:rotateY(180deg)] [backface-visibility:hidden] overflow-y-scroll">
                {credentials[index] && credentials[index].credentialSubject &&
                  Object.keys(credentials[index].credentialSubject)
                    .map((key) => {
                      if (
                        typeof credentials[index].credentialSubject[key] ===
                        'string' &&
                        credentials[index].credentialSubject[key].length > 0 &&
                        credentials[index].credentialSubject[key].length < 20
                      ) {
                        return {
                          key: (
                            key.charAt(0).toUpperCase() + key.slice(1)
                          ).replace(/([a-z0-9])([A-Z])/g, '$1 $2'),
                          value: credentials[index].credentialSubject[key],
                        };
                      }
                    })
                    .filter((item) => item !== undefined).length > 0 && (
                    <>
                      {Object.keys(credentials[index].credentialSubject)
                        .map((key) => {
                          if (
                            typeof credentials[index].credentialSubject[key] ===
                            'string' &&
                            credentials[index].credentialSubject[key].length >
                            0 &&
                            credentials[index].credentialSubject[key].length <
                            20
                          ) {
                            return {
                              key: (
                                key.charAt(0).toUpperCase() + key.slice(1)
                              ).replace(/([a-z0-9])([A-Z])/g, '$1 $2'),
                              value: credentials[index].credentialSubject[key],
                            };
                          }
                        })
                        .map((item, idx) => {
                          return (
                            <div key={idx} className="flex flex-row py-1">
                              <div className="text-gray-600 text-left w-1/2 capitalize leading-[1.1]">
                                {item?.key}
                              </div>
                              <div className="text-slate-800 text-left w-1/2 text-[#313233]">
                                {item?.value}
                              </div>
                            </div>
                          );
                        })}
                    </>
                  )}
                <div className="flex flex-row py-1">
                  <button
                    onClick={() => setModal(true)}
                    className="text-gray-500 text-center w-full capitalize leading-[1.1] underline"
                  >
                    View Credential In JSON
                  </button>
                </div>
              </div>
            </div>
          </div>
          {index !== credentials.length - 1 && (
            <button
              onClick={() => setIndex(index + 1)}
              className="text-gray-500 hover:text-gray-900 focus:outline-none absolute right-10"
            >
              <svg
                xmlns="http://www.w3.org/2000/svg"
                className="h-6 w-6 ml-2"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M9 5l7 7-7 7"
                />
              </svg>
            </button>
          )}
        </div>
        <div className="mt-10 px-12">
          <div className="flex flex-row items-center justify-center mb-5 text-gray-500">
            {policyResults[index + 1]?.policyResults.length
              ? 'The VP was verified along with:'
              : 'The VP was not verified against any policies'}
          </div>
          <div className="xs:grid xs:grid-cols-2 items-center justify-center">
            {policyResults[index + 1]?.policyResults
              .map((policy) => {
                return {
                  name:
                    policy.policy.charAt(0).toUpperCase() +
                    policy.policy.slice(1) +
                    ' Policy',
                  is_success: policy.is_success,
                };
              })
              .map((policy, idx) => {
                return (
                  <div
                    key={policy.name}
                    className={`flex items-center gap-3 overflow-hidden text-ellipsis whitespace-nowrap ${idx % 2 == 1 ? 'sm:justify-self-end' : ''}`}
                  >
                    {policy.is_success ? (
                      <CheckCircleIcon className="h-4 text-green-600" />
                    ) : (
                      <CheckCircleIcon className="h-4 text-red-600" />
                    )}
                    <div>{policy.name}</div>
                  </div>
                );
              })}
          </div>
        </div>
        <div className="flex flex-col items-center mt-12">
          <div className="flex flex-row gap-2 items-center content-center text-sm text-center text-gray-500">
            <p className="">Secured by walt.id</p>
            <WaltIcon height={15} width={15} type="gray" />
          </div>
        </div>
      </div>
    </div>
  );
}
